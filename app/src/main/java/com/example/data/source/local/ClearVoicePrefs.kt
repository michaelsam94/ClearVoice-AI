package com.example.data.source.local

import android.content.Context
import android.content.SharedPreferences

class ClearVoicePrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("clearvoice_prefs", Context.MODE_PRIVATE)

    var modelType: String
        get() = prefs.getString("key_model_type", "FAST") ?: "FAST"
        set(value) = prefs.edit().putString("key_model_type", value).apply()

    var exportFormat: String
        get() = prefs.getString("key_export_format", "WAV") ?: "WAV"
        set(value) = prefs.edit().putString("key_export_format", value).apply()

    var themePreference: String
        get() = prefs.getString("key_theme", "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString("key_theme", value).apply()
}
