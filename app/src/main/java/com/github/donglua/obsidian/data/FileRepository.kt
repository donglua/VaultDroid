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
            Log.e("Repo", "Sync error", e)
            false
        }
    }

    private fun recursiveSync(path: String) {
        Log.d("Repo", "Syncing path: $path")

        // Fetch remote list safely
        val remoteFiles = try {
            client.listFiles(path)
        } catch (e: Exception) {
            Log.e("Repo", "Failed to list remote files", e)
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
                            Log.e("Repo", "Download failed", e)
                        }
                    } else if (localTime > remoteTime + 2000) {
                        Log.d("Repo", "Pushing update: $relativePath")
                         try {
                             client.uploadFile(relativePath, localFile.readText())
                         } catch (e: Exception) {
                             Log.e("Repo", "Upload failed", e)
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
                        Log.e("Repo", "New file download failed", e)
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
                         Log.e("Repo", "Create folder failed", e)
                    }
                } else {
                    try {
                        client.uploadFile(relativePath, local.readText())
                    } catch (e: Exception) {
                         Log.e("Repo", "New file upload failed", e)
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
                 Log.e("Repo", "Force push failed", e)
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

    suspend fun createFile(path: String, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Local Create
                val parentDir = if (path.isEmpty()) localRoot else File(localRoot, path)
                if (!parentDir.exists()) parentDir.mkdirs()

                val newFile = File(parentDir, name)
                if (newFile.exists()) return@withContext false

                newFile.createNewFile()

                // 2. Remote Create (Upload empty)
                val relativePath = if (path.isEmpty()) name else "$path/$name"
                client.uploadFile(relativePath, "")

                true
            } catch (e: Exception) {
                Log.e("Repo", "Create file failed", e)
                false
            }
        }
    }

    suspend fun createFolder(path: String, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Local Create
                val parentDir = if (path.isEmpty()) localRoot else File(localRoot, path)
                if (!parentDir.exists()) parentDir.mkdirs()

                val newDir = File(parentDir, name)
                if (newDir.exists()) return@withContext false

                newDir.mkdirs()

                // 2. Remote Create
                val relativePath = if (path.isEmpty()) name else "$path/$name"
                client.createFolder(relativePath)

                true
            } catch (e: Exception) {
                Log.e("Repo", "Create folder failed", e)
                false
            }
        }
    }

    suspend fun deleteFile(path: String, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Local Delete
                val parentDir = if (path.isEmpty()) localRoot else File(localRoot, path)
                val target = File(parentDir, name)

                if (target.exists()) {
                    target.deleteRecursively()
                }

                // 2. Remote Delete
                val relativePath = if (path.isEmpty()) name else "$path/$name"
                client.delete(relativePath)

                true
            } catch (e: Exception) {
                Log.e("Repo", "Delete failed", e)
                false
            }
        }
    }

    suspend fun renameFile(path: String, oldName: String, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Local Rename
                val parentDir = if (path.isEmpty()) localRoot else File(localRoot, path)
                val source = File(parentDir, oldName)
                val dest = File(parentDir, newName)

                if (dest.exists()) return@withContext false

                if (source.exists()) {
                    source.renameTo(dest)
                }

                // 2. Remote Rename
                val relativePath = if (path.isEmpty()) oldName else "$path/$oldName"
                client.rename(relativePath, newName)

                true
            } catch (e: Exception) {
                Log.e("Repo", "Rename failed", e)
                false
            }
        }
    }
}
