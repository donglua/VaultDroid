package com.github.donglua.obsidian.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("obsidian_prefs", Context.MODE_PRIVATE)

    var webDavUrl: String
        get() = prefs.getString("url", "") ?: ""
        set(value) = prefs.edit().putString("url", value).apply()

    var username: String
        get() = prefs.getString("user", "") ?: ""
        set(value) = prefs.edit().putString("user", value).apply()

    var password: String
        get() = prefs.getString("pass", "") ?: ""
        set(value) = prefs.edit().putString("pass", value).apply()

    val isConfigured: Boolean
        get() = webDavUrl.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
}
