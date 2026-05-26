package com.michael.clearvoiceai.ui.screens.home

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michael.clearvoiceai.data.source.local.ClearVoicePrefs
import com.michael.clearvoiceai.di.DependencyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProcessState {
    object Idle : ProcessState()
    data class Loading(val progress: Float, val statusMessage: String) : ProcessState()
    data class Success(val voiceUri: String, val noiseUri: String) : ProcessState()
    data class Error(val errorMsg: String) : ProcessState()
}

class ProcessViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = ClearVoicePrefs(application)
    private val processAudioUseCase = DependencyProvider.getProcessAudioUseCase(application)

    private val _uiState = MutableStateFlow<ProcessState>(ProcessState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri = _selectedUri.asStateFlow()

    private val _voiceWaveform = MutableStateFlow<List<Float>>(emptyList())
    val voiceWaveform = _voiceWaveform.asStateFlow()

    private val _noiseWaveform = MutableStateFlow<List<Float>>(emptyList())
    val noiseWaveform = _noiseWaveform.asStateFlow()

    fun setSelectedFile(uri: Uri?) {
        _selectedUri.value = uri
        _uiState.value = ProcessState.Idle
        _voiceWaveform.value = emptyList()
        _noiseWaveform.value = emptyList()
    }

    fun startProcessing() {
        val uri = _selectedUri.value ?: run {
            _uiState.value = ProcessState.Error("Please select a file to process first.")
            return
        }

        val model = prefs.modelType
        val format = prefs.exportFormat

        _uiState.value = ProcessState.Loading(0f, "De-serializing audio stream headers...")

        viewModelScope.launch {
            try {
                _voiceWaveform.value = emptyList()
                _noiseWaveform.value = emptyList()
                
                val result = processAudioUseCase(uri.toString(), model, format) { progress ->
                    _uiState.value = ProcessState.Loading(progress, "Extracting audio and reducing background hum...")
                    generateMockWaveforms(progress)
                }
                
                _uiState.value = ProcessState.Success(result.first, result.second)
            } catch (e: Exception) {
                Log.e("ProcessViewModel", "Error processing audio: ${e.message}", e)
                _uiState.value = ProcessState.Error(e.message ?: "Unknown decoding error occurred.")
            }
        }
    }

    private fun generateMockWaveforms(progress: Float) {
        val totalBars = 35
        val listVoice = ArrayList<Float>(totalBars)
        val listNoise = ArrayList<Float>(totalBars)
        
        val seed = (progress * 133).toInt()
        for (i in 0 until totalBars) {
            val voiceAmp = (Math.sin((i.toDouble() / totalBars) * Math.PI * 4 + seed * 0.05).toFloat().coerceIn(-1f, 1f) * 0.35f + 0.45f) + (Math.random().toFloat() * 0.12f)
            val noiseAmp = (Math.cos((i.toDouble() / totalBars) * Math.PI * 5 + seed * 0.07).toFloat().coerceIn(-1f, 1f) * 0.15f + 0.2f) + (Math.random().toFloat() * 0.08f)
            listVoice.add(voiceAmp.coerceIn(0f, 1f))
            listNoise.add(noiseAmp.coerceIn(0f, 1f))
        }
        _voiceWaveform.value = listVoice
        _noiseWaveform.value = listNoise
    }
}
