package com.example.obsidian.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.obsidian.data.FileRepository
import com.example.obsidian.data.Prefs
import com.example.obsidian.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: FileRepository
    private lateinit var adapter: FileAdapter
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs(this)
        if (!prefs.isConfigured) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        repo = FileRepository(this)

        adapter = FileAdapter(emptyList()) { file ->
            if (file.isDirectory) {
                val newPath = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
                openFolder(newPath)
            } else {
                val intent = Intent(this, ViewerActivity::class.java)
                val relative = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
                intent.putExtra("path", relative)
                startActivity(intent)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabSync.setOnClickListener {
            sync()
        }

        refreshFiles()
        sync()
    }

    private fun openFolder(path: String) {
        currentPath = path
        refreshFiles()
    }

    private fun refreshFiles() {
        val files = repo.getLocalFiles(currentPath)
        adapter.updateFiles(files)
        title = if (currentPath.isEmpty()) getString(com.example.obsidian.R.string.title_files) else currentPath
    }

    override fun onBackPressed() {
        if (currentPath.isNotEmpty()) {
            val parent = File(filesDir, currentPath).parentFile
            if (parent == null || parent == filesDir) {
                currentPath = ""
            } else {
                 currentPath = parent.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
            }
            refreshFiles()
        } else {
            super.onBackPressed()
        }
    }

    private fun sync() {
        Toast.makeText(this, getString(com.example.obsidian.R.string.msg_syncing), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val success = repo.syncAll()
            if (success) {
                Toast.makeText(this@MainActivity, "Sync Complete", Toast.LENGTH_SHORT).show()
                refreshFiles()
            } else {
                Toast.makeText(this@MainActivity, "Sync Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
