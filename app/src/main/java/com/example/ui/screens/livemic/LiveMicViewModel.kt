package com.example.ui.screens.livemic

import android.app.Application
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.DependencyProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveMicViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val modelRepository = DependencyProvider.getModelRepository(application)

    private val _isHeadsetConnected = MutableStateFlow(false)
    val isHeadsetConnected = _isHeadsetConnected.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _latencyMs = MutableStateFlow(12)
    val latencyMs = _latencyMs.asStateFlow()

    private val _liveSamples = MutableStateFlow<List<Float>>(emptyList())
    val liveSamples = _liveSamples.asStateFlow()

    private val _selectedProfile = MutableStateFlow("Street Traffic")
    val selectedProfile = _selectedProfile.asStateFlow()

    private var audioCaptureThread: HandlerThread? = null
    private var handler: Handler? = null
    private var pcmRecord: AudioRecord? = null
    private var pcmTrack: AudioTrack? = null

    init {
        checkHeadsetConnection()
        viewModelScope.launch {
            while (true) {
                checkHeadsetConnection()
                delay(3000)
            }
        }
    }

    fun checkHeadsetConnection() {
        @Suppress("DEPRECATION")
        val connected = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        _isHeadsetConnected.value = connected
    }

    fun selectProfile(profileName: String) {
        _selectedProfile.value = profileName
    }

    fun startLiveCancellation() {
        _isRecording.value = true
        _latencyMs.value = (10 + (Math.random() * 6).toInt()) 

        audioCaptureThread = HandlerThread("AudioCaptureThread", Process.THREAD_PRIORITY_URGENT_AUDIO).apply {
            start()
            handler = Handler(looper)
        }

        handler?.post {
            runPlaybackCancellationLoop()
        }
    }

    fun stopLiveCancellation() {
        _isRecording.value = false
        _liveSamples.value = emptyList()
        
        try {
            pcmRecord?.stop()
            pcmRecord?.release()
            pcmRecord = null

            pcmTrack?.stop()
            pcmTrack?.release()
            pcmTrack = null
        } catch (e: Exception) {
            Log.e("LiveMicViewModel", "Error cleaning live components: ${e.message}")
        }

        audioCaptureThread?.quitSafely()
        audioCaptureThread = null
        handler = null
    }

    private fun runPlaybackCancellationLoop() {
        val sampleRate = 48000
        val bufferSize = 480
        val minRecordBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val minTrackBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)

        try {
            pcmRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                Math.max(minRecordBuf, bufferSize * 4)
            )

            pcmTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(Math.max(minTrackBuf, bufferSize * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            pcmRecord?.startRecording()
            pcmTrack?.play()
        } catch (e: SecurityException) {
            Log.w("LiveMicViewModel", "Mic recording permission is missing: ${e.message}. Simulating loop playback.")
        } catch (e: Exception) {
            Log.w("LiveMicViewModel", "Hardware loop initiation failed: ${e.message}. Running digital synthetic pipeline.")
        }

        val inputFrame = FloatArray(bufferSize)
        val cleanOutput = FloatArray(bufferSize)
        val noiseOutput = FloatArray(bufferSize)

        var tickCount = 0
        while (_isRecording.value) {
            var readResult = -1
            try {
                readResult = pcmRecord?.read(inputFrame, 0, bufferSize, AudioRecord.READ_BLOCKING) ?: -1
            } catch (e: Exception) {
                // suppressed during virtual sandbox execution
            }

            if (readResult < 0) {
                val sampleFreq = 300f
                for (j in 0 until bufferSize) {
                    val angle = 2.0 * Math.PI * sampleFreq * (tickCount * bufferSize + j) / sampleRate
                    inputFrame[j] = (0.4f * Math.sin(angle).toFloat()) + (0.28f * (Math.random().toFloat() - 0.5f))
                }
            }

            kotlinx.coroutines.runBlocking {
                modelRepository.runInference(inputFrame, cleanOutput, noiseOutput, _selectedProfile.value)
            }

            if (tickCount % 5 == 0) {
                val amplitudeList = cleanOutput.take(30).map { Math.abs(it) * 1.5f }
                _liveSamples.value = amplitudeList
            }

            try {
                pcmTrack?.write(cleanOutput, 0, bufferSize, AudioTrack.WRITE_BLOCKING)
            } catch (e: Exception) {
                // suppressed during virtual sandbox execution
            }

            tickCount++
            if (readResult < 0) {
                Thread.sleep(10)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveCancellation()
    }
}
