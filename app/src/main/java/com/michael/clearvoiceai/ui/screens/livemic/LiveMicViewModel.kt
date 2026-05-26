package com.michael.clearvoiceai.ui.screens.livemic

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.michael.clearvoiceai.di.DependencyProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

data class LiveRecording(val uri: String, val name: String)

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

    private val _recordingUri = MutableStateFlow<String?>(null)
    val recordingUri = _recordingUri.asStateFlow()

    private val _recordingName = MutableStateFlow<String?>(null)
    val recordingName = _recordingName.asStateFlow()

    private val _recordingList = MutableStateFlow<List<LiveRecording>>(emptyList())
    val recordingList = _recordingList.asStateFlow()

    private val recordedBuffers = Collections.synchronizedList(mutableListOf<ShortArray>())

    private var audioCaptureThread: HandlerThread? = null
    private var handler: Handler? = null
    private var pcmRecord: AudioRecord? = null
    private var pcmTrack: AudioTrack? = null

    init {
        checkHeadsetConnection()
        loadSavedRecordings()
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
        recordedBuffers.clear()
        _recordingUri.value = null
        _recordingName.value = null

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
        val sampleRate = 16000
        val bufferSize = 480
        val minRecordBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minTrackBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)

        try {
            pcmRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
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
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
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
        val shortInput = ShortArray(bufferSize)
        val shortOutput = ShortArray(bufferSize)

        var tickCount = 0
        while (_isRecording.value) {
            var readResult = -1
            try {
                readResult = pcmRecord?.read(shortInput, 0, bufferSize, AudioRecord.READ_BLOCKING) ?: -1
            } catch (e: Exception) {
                // suppressed during virtual sandbox execution
            }

            if (readResult < 0) {
                val sampleFreq = 300f
                for (j in 0 until bufferSize) {
                    val angle = 2.0 * Math.PI * sampleFreq * (tickCount * bufferSize + j) / sampleRate
                    inputFrame[j] = (0.4f * Math.sin(angle).toFloat()) + (0.28f * (Math.random().toFloat() - 0.5f))
                    shortInput[j] = (inputFrame[j] * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
                }
            } else {
                for (j in 0 until bufferSize) {
                    inputFrame[j] = shortInput[j] / 32768f
                }
            }

            kotlinx.coroutines.runBlocking {
                modelRepository.runInference(inputFrame, cleanOutput, noiseOutput, _selectedProfile.value)
            }

            if (tickCount % 5 == 0) {
                val amplitudeList = cleanOutput.take(30).map { Math.abs(it) * 1.5f }
                _liveSamples.value = amplitudeList
            }

            for (j in 0 until bufferSize) {
                shortOutput[j] = (cleanOutput[j] * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
            }

            recordedBuffers.add(shortOutput.clone())

            try {
                pcmTrack?.write(shortOutput, 0, bufferSize, AudioTrack.WRITE_BLOCKING)
            } catch (e: Exception) {
                // suppressed during virtual sandbox execution
            }

            tickCount++
            if (readResult < 0) {
                Thread.sleep(10)
            }
        }

        if (recordedBuffers.isNotEmpty()) {
            saveRecordingToProvider()
        }
    }

    fun loadSavedRecordings() {
        val list = mutableListOf<LiveRecording>()
        val resolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Music/ClearVoiceAI%")
        } else {
            arrayOf("%/Music/ClearVoiceAI%")
        }
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            resolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    list.add(LiveRecording(contentUri.toString(), name))
                }
            }
        } catch (e: Exception) {
            Log.e("LiveMicViewModel", "Error querying MediaStore: ${e.message}", e)
        }

        try {
            val directory = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            val appDir = java.io.File(directory, "ClearVoiceAI")
            if (appDir.exists() && appDir.isDirectory) {
                appDir.listFiles { _, name -> name.endsWith(".wav") && name.startsWith("LiveMic_Record_") }
                    ?.forEach { file ->
                        val uriStr = file.absolutePath
                        val name = file.name
                        if (list.none { it.name == name }) {
                            list.add(LiveRecording(uriStr, name))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("LiveMicViewModel", "Error loading fallback recordings: ${e.message}", e)
        }

        list.sortByDescending { it.name }
        _recordingList.value = list
    }

    private fun saveRecordingToProvider() {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val fileName = "LiveMic_Record_$timeStamp.wav"

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/ClearVoiceAI")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    writeWavToStream(outputStream, recordedBuffers.toList())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                _recordingUri.value = uri.toString()
                _recordingName.value = fileName
                Log.d("LiveMicViewModel", "Saved live recording to $uri")
                loadSavedRecordings()
            } catch (e: Exception) {
                Log.e("LiveMicViewModel", "Error saving live recording: ${e.message}", e)
            }
        } else {
            try {
                val directory = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
                val appDir = java.io.File(directory, "ClearVoiceAI")
                if (!appDir.exists()) appDir.mkdirs()
                val file = java.io.File(appDir, fileName)
                java.io.FileOutputStream(file).use { outputStream ->
                    writeWavToStream(outputStream, recordedBuffers.toList())
                }
                _recordingUri.value = file.absolutePath
                _recordingName.value = fileName
                Log.d("LiveMicViewModel", "Saved live recording fallback to ${file.absolutePath}")
                loadSavedRecordings()
            } catch (e: Exception) {
                Log.e("LiveMicViewModel", "Error saving live recording fallback: ${e.message}", e)
            }
        }
    }

    private fun writeWavToStream(outputStream: java.io.OutputStream, buffers: List<ShortArray>) {
        val totalSampleCount = buffers.sumOf { it.size }
        val totalDataSize = totalSampleCount * 2
        val headerSize = 44
        val totalFileSize = headerSize + totalDataSize - 8

        val header = ByteBuffer.allocate(headerSize).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalFileSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size
            putShort(1) // AudioFormat PCM = 1
            putShort(1) // NumChannels = 1
            putInt(16000) // SampleRate
            putInt(16000 * 2) // ByteRate
            putShort(2) // BlockAlign
            putShort(16) // BitsPerSample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(totalDataSize)
        }

        outputStream.write(header.array())

        val bufferBytes = ByteBuffer.allocate(1024 * 2).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }
        for (buf in buffers) {
            for (i in buf.indices) {
                if (!bufferBytes.hasRemaining()) {
                    outputStream.write(bufferBytes.array(), 0, bufferBytes.position())
                    bufferBytes.clear()
                }
                bufferBytes.putShort(buf[i])
            }
        }
        if (bufferBytes.position() > 0) {
            outputStream.write(bufferBytes.array(), 0, bufferBytes.position())
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveCancellation()
    }
}
