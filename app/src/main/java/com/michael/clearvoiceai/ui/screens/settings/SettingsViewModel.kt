package com.michael.clearvoiceai.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.michael.clearvoiceai.data.source.local.ClearVoicePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = ClearVoicePrefs(application)

    private val _selectedModel = MutableStateFlow(prefs.modelType)
    val selectedModel = _selectedModel.asStateFlow()

    private val _exportFormat = MutableStateFlow(prefs.exportFormat)
    val exportFormat = _exportFormat.asStateFlow()

    private val _themePref = MutableStateFlow(prefs.themePreference)
    val themePref = _themePref.asStateFlow()

    fun updateModelType(type: String) {
        prefs.modelType = type
        _selectedModel.value = type
    }

    fun updateExportFormat(format: String) {
        prefs.exportFormat = format
        _exportFormat.value = format
    }

    fun updateThemePreference(theme: String) {
        prefs.themePreference = theme
        _themePref.value = theme
    }
}
