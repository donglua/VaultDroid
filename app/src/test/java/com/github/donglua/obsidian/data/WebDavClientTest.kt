package com.github.donglua.obsidian.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class WebDavClientTest {

    @Test
    fun parsePropFind_validXml_returnsFiles() {
        val xml = """
<?xml version="1.0"?>
<d:multistatus xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns" xmlns:oc="http://owncloud.org/ns">
    <d:response>
        <d:href>/remote.php/webdav/Obsidian%20Vault/</d:href>
        <d:propstat>
            <d:prop>
                <d:getlastmodified>Tue, 22 Aug 2023 18:02:35 GMT</d:getlastmodified>
                <d:resourcetype>
                    <d:collection/>
                </d:resourcetype>
            </d:prop>
            <d:status>HTTP/1.1 200 OK</d:status>
        </d:propstat>
    </d:response>
    <d:response>
        <d:href>/remote.php/webdav/Obsidian%20Vault/Note.md</d:href>
        <d:propstat>
            <d:prop>
                <d:getlastmodified>Mon, 21 Aug 2023 10:00:00 GMT</d:getlastmodified>
                <d:resourcetype/>
            </d:prop>
            <d:status>HTTP/1.1 200 OK</d:status>
        </d:propstat>
    </d:response>
</d:multistatus>
        """.trimIndent()

        val files = WebDavClient.parsePropFind(xml)

        assertEquals("Should parse 2 files", 2, files.size)

        // Check folder
        val folder = files.find { it.isFolder }
        assertNotNull("Folder should be found", folder)
        assertEquals("Obsidian Vault", folder?.name)
        assertTrue(folder!!.lastModified > 0)

        // Check file
        val file = files.find { !it.isFolder }
        assertNotNull("File should be found", file)
        assertEquals("Note.md", file?.name)
        assertTrue(file!!.lastModified > 0)
    }

    @Test
    fun parsePropFind_emptyResponse_returnsEmptyList() {
        val xml = ""
        val files = WebDavClient.parsePropFind(xml)
        assertTrue(files.isEmpty())
    }
}
