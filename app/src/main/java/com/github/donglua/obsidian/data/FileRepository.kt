package com.github.donglua.obsidian.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository(context: Context) {

    private val prefs = Prefs(context)
    private val client = WebDavClient(prefs)
    private val localRoot = context.filesDir

    suspend fun syncAll(): Boolean {
        if (!prefs.isConfigured) return false
        return try {
            withContext(Dispatchers.IO) {
                recursiveSync("")
            }
            true
        } catch (e: Exception) {
            Log.e("Repo", "Sync error")
            false
        }
    }

    private fun recursiveSync(path: String) {
        Log.d("Repo", "Syncing path: $path")

        // Fetch remote list safely
        val remoteFiles = try {
            client.listFiles(path)
        } catch (e: Exception) {
            Log.e("Repo", "Failed to list remote files")
            emptyList()
        }

        // Ensure local directory exists
        val localDir = if (path.isEmpty()) localRoot else File(localRoot, path)
        if (!localDir.exists()) localDir.mkdirs()

        val localFiles = localDir.listFiles() ?: emptyArray()

        // 1. Process Remote Files (Pull updates or new files)
        for (remote in remoteFiles) {
            val relativePath = if (path.isEmpty()) remote.name else "$path/${remote.name}"
            val localFile = localFiles.find { it.name == remote.name }

            if (remote.isFolder) {
                recursiveSync(relativePath)
            } else {
                if (localFile != null) {
                    // Conflict Resolution by Timestamp
                    val localTime = localFile.lastModified()
                    val remoteTime = remote.lastModified

                    // Threshold of 2 seconds for clock skew
                    if (remoteTime > localTime + 2000) {
                        Log.d("Repo", "Pulling update: $relativePath")
                        try {
                             val content = client.downloadFile(relativePath)
                             localFile.writeText(content)
                             localFile.setLastModified(remoteTime)
                        } catch (e: Exception) {
                            Log.e("Repo", "Download failed")
                        }
                    } else if (localTime > remoteTime + 2000) {
                        Log.d("Repo", "Pushing update: $relativePath")
                         try {
                             client.uploadFile(relativePath, localFile.readText())
                         } catch (e: Exception) {
                             Log.e("Repo", "Upload failed")
                         }
                    }
                } else {
                    // New Remote File -> Download
                    Log.d("Repo", "Downloading new file: $relativePath")
                    try {
                        val content = client.downloadFile(relativePath)
                        val newFile = File(localRoot, relativePath)
                        newFile.writeText(content)
                        newFile.setLastModified(remote.lastModified)
                    } catch (e: Exception) {
                        Log.e("Repo", "New file download failed")
                    }
                }
            }
        }

        // 2. Process Local Files (Push new files)
        for (local in localFiles) {
            val remote = remoteFiles.find { it.name == local.name }
            if (remote == null) {
                val relativePath = if (path.isEmpty()) local.name else "$path/${local.name}"
                Log.d("Repo", "Pushing new item: $relativePath")
                if (local.isDirectory) {
                    try {
                        client.createFolder(relativePath)
                        recursiveSync(relativePath)
                    } catch (e: Exception) {
                         Log.e("Repo", "Create folder failed")
                    }
                } else {
                    try {
                        client.uploadFile(relativePath, local.readText())
                    } catch (e: Exception) {
                         Log.e("Repo", "New file upload failed")
                    }
                }
            }
        }
    }

    suspend fun syncUp(path: String, content: String) {
        // Force push single file (called from Viewer on save)
        withContext(Dispatchers.IO) {
             try {
                 client.uploadFile(path, content)
                 // Update local timestamp to avoid immediate overwrite on next sync?
                 // No, if we push, server timestamp updates to 'now'.
                 // We should ideally fetch new ETag or timestamp.
                 // But for MVP, we just push. Next sync will see remoteTime ~ localTime (if we assume clock sync).
             } catch (e: Exception) {
                 Log.e("Repo", "Force push failed")
             }
        }
    }

    fun getLocalFiles(path: String = ""): List<File> {
        val folder = if (path.isEmpty()) localRoot else File(localRoot, path)
        return folder.listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
    }

    fun getFileContent(path: String): String {
        val file = File(localRoot, path)
        return if (file.exists()) file.readText() else ""
    }

    fun saveLocalFile(path: String, content: String) {
        val file = File(localRoot, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        // Does not push automatically? Viewer calls syncUp? No.
        // Viewer only saves locally.
        // User must "Manual Sync" to push.
        // This is consistent with requirement.
    }
}
