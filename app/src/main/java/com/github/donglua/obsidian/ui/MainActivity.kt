package com.github.donglua.obsidian.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.github.donglua.obsidian.data.FileRepository
import com.github.donglua.obsidian.data.Prefs
import com.github.donglua.obsidian.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: FileRepository
    private lateinit var adapter: FileAdapter
    private var currentPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_sort_by_size)

        val prefs = Prefs(this)
        if (!prefs.isConfigured) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        repo = FileRepository(this)

        adapter = FileAdapter(
            files = emptyList(),
            onClick = { file ->
                if (file.isDirectory) {
                    val newPath = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
                    openFolder(newPath)
                } else {
                    val intent = Intent(this, ViewerActivity::class.java)
                    val relative = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
                    intent.putExtra("path", relative)
                    startActivity(intent)
                }
            },
            onLongClick = { file ->
                val index = adapter.files.indexOfFirst { it.absolutePath == file.absolutePath }
                val holder = binding.recyclerView.findViewHolderForAdapterPosition(index)
                val anchor = holder?.itemView ?: binding.recyclerView

                val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
                popup.menu.add(0, 1, 0, getString(com.github.donglua.obsidian.R.string.action_rename))
                popup.menu.add(0, 2, 0, getString(com.github.donglua.obsidian.R.string.action_delete))
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> showRenameDialog(file)
                        2 -> showDeleteDialog(file)
                    }
                    true
                }
                popup.show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fabSync.setOnClickListener {
            sync()
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                com.github.donglua.obsidian.R.id.nav_new_note -> showNewNoteDialog()
                com.github.donglua.obsidian.R.id.nav_new_folder -> showNewFolderDialog()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        refreshFiles()
        sync()
    }

    private fun showNewNoteDialog() {
        val input = android.widget.EditText(this)
        input.hint = getString(com.github.donglua.obsidian.R.string.dialog_hint_name)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_new_note))
            .setView(input)
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_create)) { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    val finalName = if (name.endsWith(".md")) name else "$name.md"
                    lifecycleScope.launch {
                        val success = repo.createFile(currentPath, finalName)
                        if (success) {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_success), Toast.LENGTH_SHORT).show()
                            refreshFiles()
                            sync()
                        } else {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_cancel), null)
            .show()
    }

    private fun showNewFolderDialog() {
        val input = android.widget.EditText(this)
        input.hint = getString(com.github.donglua.obsidian.R.string.dialog_hint_name)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_new_folder))
            .setView(input)
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_create)) { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        val success = repo.createFolder(currentPath, name)
                        if (success) {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_success), Toast.LENGTH_SHORT).show()
                            refreshFiles()
                            sync()
                        } else {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_cancel), null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val input = android.widget.EditText(this)
        input.hint = getString(com.github.donglua.obsidian.R.string.dialog_hint_name)
        input.setText(file.name)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_rename))
            .setView(input)
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_rename)) { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty() && newName != file.name) {
                    lifecycleScope.launch {
                        val success = repo.renameFile(currentPath, file.name, newName)
                        if (success) {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_success), Toast.LENGTH_SHORT).show()
                            refreshFiles()
                            sync()
                        } else {
                            Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_cancel), null)
            .show()
    }

    private fun showDeleteDialog(file: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_delete))
            .setMessage(getString(com.github.donglua.obsidian.R.string.dialog_message_delete, file.name))
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_delete)) { _, _ ->
                lifecycleScope.launch {
                    val success = repo.deleteFile(currentPath, file.name)
                    if (success) {
                        Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_success), Toast.LENGTH_SHORT).show()
                        refreshFiles()
                        sync()
                    } else {
                        Toast.makeText(this@MainActivity, getString(com.github.donglua.obsidian.R.string.msg_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_cancel), null)
            .show()
    }

    private fun openFolder(path: String) {
        currentPath = path
        refreshFiles()
    }

    private fun refreshFiles() {
        val files = repo.getLocalFiles(currentPath)
        adapter.updateFiles(files)
        title = if (currentPath.isEmpty()) getString(com.github.donglua.obsidian.R.string.title_files) else currentPath
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (currentPath.isNotEmpty()) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sync() {
        Toast.makeText(this, getString(com.github.donglua.obsidian.R.string.msg_syncing), Toast.LENGTH_SHORT).show()
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
