package com.github.donglua.obsidian.data

import android.util.Log
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RemoteFile(
    val href: String,
    val name: String,
    val isFolder: Boolean,
    val lastModified: Long = 0
)

class WebDavClient(private val prefs: Prefs) {

    private val client = OkHttpClient.Builder().build()

    private fun getAuthHeader(): String {
        return Credentials.basic(prefs.username, prefs.password)
    }

    private fun getBaseUrl(): String {
        var url = prefs.webDavUrl
        if (!url.endsWith("/")) url += "/"
        return url
    }

    private fun buildFullUrl(path: String): String {
        return getBaseUrl() + path.trimStart('/')
    }

    fun listFiles(path: String = ""): List<RemoteFile> {
        var fullUrl = buildFullUrl(path)
        if (path.isNotEmpty() && !fullUrl.endsWith("/")) {
            fullUrl += "/"
        }

        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", getAuthHeader())
            .header("Depth", "1")
            .method("PROPFIND", "".toRequestBody(null))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return emptyList()
            if (!response.isSuccessful) throw IOException("WebDAV Error: ${response.code}")
            val body = response.body?.string() ?: ""
            val allFiles = parsePropFind(body)

            // Filter out self-reference (infinite recursion fix)
            // Strategy: The requested folder is always the shortest href in the response.
            if (allFiles.isNotEmpty()) {
                val shortest = allFiles.minByOrNull { it.href.length }
                if (shortest != null) {
                    return allFiles.filter { it != shortest }
                }
            }
            return allFiles
        }
    }

    fun downloadFile(path: String): String {
        val fullUrl = buildFullUrl(path)
        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", getAuthHeader())
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download Error: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun uploadFile(path: String, content: String) {
        val fullUrl = buildFullUrl(path)
        val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", getAuthHeader())
            .put(content.toRequestBody("text/markdown".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Upload Error: ${response.code}")
        }
    }

    fun createFolder(path: String) {
         val fullUrl = buildFullUrl(path)
         val request = Request.Builder()
            .url(fullUrl)
            .header("Authorization", getAuthHeader())
            .method("MKCOL", null)
            .build()
         client.newCall(request).execute().use { response ->
             if (!response.isSuccessful && response.code != 405) {
                 throw IOException("Failed to create folder at $path, status: ${response.code}")
             }
         }
    }

    private fun parsePropFind(xml: String): List<RemoteFile> {
        val files = mutableListOf<RemoteFile>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentHref = ""
            var currentIsFolder = false
            var currentLastModified = 0L
            var inResponse = false

            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name.contains("response", ignoreCase = true)) {
                            inResponse = true
                            currentHref = ""
                            currentIsFolder = false
                            currentLastModified = 0L
                        } else if (inResponse) {
                            if (name.contains("href", ignoreCase = true)) {
                                try { currentHref = parser.nextText() } catch (e: Exception) {}
                            } else if (name.contains("collection", ignoreCase = true)) {
                                currentIsFolder = true
                            } else if (name.contains("getlastmodified", ignoreCase = true)) {
                                try {
                                    val dateStr = parser.nextText()
                                    currentLastModified = dateFormat.parse(dateStr)?.time ?: 0L
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name.contains("response", ignoreCase = true)) {
                            if (currentHref.isNotEmpty() && inResponse) {
                                try {
                                    val decodedHref = URLDecoder.decode(currentHref, "UTF-8")
                                    val fileName = decodedHref.trimEnd('/').substringAfterLast('/')

                                    if (fileName.isNotEmpty()) {
                                         files.add(RemoteFile(decodedHref, fileName, currentIsFolder, currentLastModified))
                                    }
                                } catch (e: Exception) {
                                    Log.e("WebDav", "Href decode error", e)
                                }
                            }
                            inResponse = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("WebDav", "XML Parse error", e)
        }
        return files
    }
}
