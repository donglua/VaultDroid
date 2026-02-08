package com.github.donglua.obsidian.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.github.donglua.obsidian.R.layout.activity_viewer)

        val path = intent.getStringExtra("path") ?: ""
        if (path.isEmpty()) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment = ViewerFragment.newInstance(path)
            supportFragmentManager.beginTransaction()
                .replace(com.github.donglua.obsidian.R.id.fragment_container, fragment)
                .commit()
        }
    }
}
