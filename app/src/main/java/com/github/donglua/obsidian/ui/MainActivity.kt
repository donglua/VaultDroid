package com.github.donglua.obsidian.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
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

        val prefs = Prefs(this)
        if (!prefs.isConfigured) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        repo = FileRepository(this)

        adapter = TreeFileAdapter(emptyList()) { node ->
            if (node.file.isDirectory) {
                TreeManager.toggle(node.file)
                refreshFiles()
            } else {
                openFile(node.file)
            }
        }

        binding.recyclerViewTree.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTree.adapter = adapter

        binding.fabSync.setOnClickListener {
            sync()
        }

        refreshFiles()
        sync()
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
