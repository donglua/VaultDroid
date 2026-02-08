package com.github.donglua.obsidian.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
    private lateinit var adapter: TreeFileAdapter

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

        adapter = TreeFileAdapter(
            nodes = emptyList(),
            onClick = { node ->
                if (node.file.isDirectory) {
                    TreeManager.toggle(node.file)
                    refreshFiles()
                } else {
                    openFile(node.file)
                }
            },
            onLongClick = { view, node ->
                val popup = androidx.appcompat.widget.PopupMenu(this, view)
                popup.menu.add(0, 1, 0, getString(com.github.donglua.obsidian.R.string.action_rename))
                popup.menu.add(0, 2, 0, getString(com.github.donglua.obsidian.R.string.action_delete))
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> showRenameDialog(node.file)
                        2 -> showDeleteDialog(node.file)
                    }
                    true
                }
                popup.show()
            }
        )

        // Binding for recyclerViewTree might be nested or direct depending on layout structure.
        // In flat file, it's direct.
        binding.recyclerViewTree.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTree.adapter = adapter

        binding.fabSync.setOnClickListener {
            sync()
        }

        // Sidebar Buttons
        findViewById<Button>(com.github.donglua.obsidian.R.id.btn_new_note)?.setOnClickListener {
            showNewNoteDialog()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<Button>(com.github.donglua.obsidian.R.id.btn_new_folder)?.setOnClickListener {
            showNewFolderDialog()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
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
                        // Create in Root for now
                        val success = repo.createFile("", finalName)
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
                         // Create in Root
                        val success = repo.createFolder("", name)
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

        val relativePath = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
        val parentPath = relativePath.substringBeforeLast('/', "")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_rename))
            .setView(input)
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_rename)) { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty() && newName != file.name) {
                    lifecycleScope.launch {
                        val success = repo.renameFile(parentPath, file.name, newName)
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
        val relativePath = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')
        val parentPath = relativePath.substringBeforeLast('/', "")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(com.github.donglua.obsidian.R.string.dialog_title_delete))
            .setMessage(getString(com.github.donglua.obsidian.R.string.dialog_message_delete, file.name))
            .setPositiveButton(getString(com.github.donglua.obsidian.R.string.dialog_btn_delete)) { _, _ ->
                lifecycleScope.launch {
                    val success = repo.deleteFile(parentPath, file.name)
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

    private fun refreshFiles() {
        val nodes = TreeManager.buildTree(filesDir)
        adapter.updateNodes(nodes)
    }

    private fun openFile(file: File) {
        val relativePath = file.absolutePath.substringAfter(filesDir.absolutePath).trimStart('/')

        if (isTablet()) {
            val fragment = ViewerFragment.newInstance(relativePath)
            supportFragmentManager.beginTransaction()
                .replace(com.github.donglua.obsidian.R.id.fragment_container, fragment)
                .commit()

            // Hide placeholder
            binding.tvPlaceholder.visibility = View.GONE
        } else {
            val intent = Intent(this, ViewerActivity::class.java)
            intent.putExtra("path", relativePath)
            startActivity(intent)

            // Close drawer on phone after selection
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun isTablet(): Boolean {
        return resources.getBoolean(com.github.donglua.obsidian.R.bool.is_tablet)
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
