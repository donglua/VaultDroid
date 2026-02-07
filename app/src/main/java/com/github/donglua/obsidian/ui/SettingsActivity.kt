package com.github.donglua.obsidian.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.donglua.obsidian.data.Prefs
import com.github.donglua.obsidian.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        binding.editUrl.setText(prefs.webDavUrl)
        binding.editUser.setText(prefs.username)
        binding.editPass.setText(prefs.password)

        binding.btnSave.setOnClickListener {
            val url = binding.editUrl.text.toString().trim()
            val user = binding.editUser.text.toString().trim()
            val pass = binding.editPass.text.toString()

            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.webDavUrl = url
            prefs.username = user
            prefs.password = pass

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
