package com.michael.clearvoiceai.data.repository

import android.content.Context
import android.util.Log
import com.michael.clearvoiceai.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class ModelRepositoryImpl(
    private val context: Context
) : ModelRepository {

    private var interpreter: Interpreter? = null
    private var modelLoaded: Boolean = false

    // Multi-band filters for DSP processing (sample rate assumed 16000Hz)
    private val sampleRate = 16000.0
    
    // Band 1: Low-Mid (150Hz to 800Hz) - Center 475Hz, Q=0.73
    private val filterBand1 = BiquadFilter(BiquadFilter.Type.BANDPASS, sampleRate, 475.0, 0.73)
    
    // Band 2: Mid (800Hz to 2500Hz) - Center 1650Hz, Q=0.97
    private val filterBand2 = BiquadFilter(BiquadFilter.Type.BANDPASS, sampleRate, 1650.0, 0.97)
    
    // Band 3: High-Mid (2500Hz to 5000Hz) - Center 3750Hz, Q=1.5
    private val filterBand3 = BiquadFilter(BiquadFilter.Type.BANDPASS, sampleRate, 3750.0, 1.5)
    
    // Output cleanup filters (Butterworth lowpass and highpass)
    private val outputHighpass = BiquadFilter(BiquadFilter.Type.HIGHPASS, sampleRate, 120.0, 0.707)
    private val outputLowpass = BiquadFilter(BiquadFilter.Type.LOWPASS, sampleRate, 5500.0, 0.707)

    // State trackers for multiband noise reduction
    private val noiseFloors = floatArrayOf(-1f, -1f, -1f)
    private val currentGains = floatArrayOf(0.1f, 0.1f, 0.1f)

    init {
        try {
            val assetManager = context.assets
            val modelName = "rnnoise.tflite"
            val fileDescriptor = assetManager.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val buffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(buffer, options)
            modelLoaded = true
            Log.d("ModelRepositoryImpl", "TFLite Model $modelName loaded successfully.")
        } catch (e: Exception) {
            Log.w("ModelRepositoryImpl", "TFLite model not found or incompatible, falling back to native high-fidelity DSP filtering: ${e.message}")
        }
    }

    override fun reset() {
        filterBand1.reset()
        filterBand2.reset()
        filterBand3.reset()
        outputHighpass.reset()
        outputLowpass.reset()
        
        noiseFloors[0] = -1f
        noiseFloors[1] = -1f
        noiseFloors[2] = -1f
        
        currentGains[0] = 0.1f
        currentGains[1] = 0.1f
        currentGains[2] = 0.1f
    }

    override suspend fun runInference(
        inputFrame: FloatArray,
        cleanOutput: FloatArray,
        noiseOutput: FloatArray,
        modelType: String
    ): Unit = withContext(Dispatchers.Default) {
        if (modelLoaded && interpreter != null) {
            try {
                val inputVal = arrayOf(inputFrame)
                val outputMap = mutableMapOf<Int, Any>()
                outputMap[0] = arrayOf(cleanOutput)
                outputMap[1] = arrayOf(noiseOutput)
                interpreter?.runForMultipleInputsOutputs(arrayOf(inputVal), outputMap)
                return@withContext
            } catch (e: Exception) {
                // Log and gracefully fall back to our advanced DSP implementation
            }
        }

        // --- ADVANCED OFFLINE MULTI-BAND SPECTRUM DSP SPEECH ISOLATOR ---
        val size = inputFrame.size
        
        // 1. Analyze and segment signal into 3 frequency bands
        val b1Samples = DoubleArray(size)
        val b2Samples = DoubleArray(size)
        val b3Samples = DoubleArray(size)
        
        var b1Power = 0.0
        var b2Power = 0.0
        var b3Power = 0.0
        
        for (i in 0 until size) {
            val sample = inputFrame[i].toDouble()
            b1Samples[i] = filterBand1.process(sample)
            b2Samples[i] = filterBand2.process(sample)
            b3Samples[i] = filterBand3.process(sample)
            
            b1Power += b1Samples[i] * b1Samples[i]
            b2Power += b2Samples[i] * b2Samples[i]
            b3Power += b3Samples[i] * b3Samples[i]
        }
        
        // Compute RMS for each band
        val rms1 = Math.sqrt(b1Power / size).toFloat()
        val rms2 = Math.sqrt(b2Power / size).toFloat()
        val rms3 = Math.sqrt(b3Power / size).toFloat()
        val bandRms = floatArrayOf(rms1, rms2, rms3)
        
        // Thresholds based on selected profile
        val noiseGateMultiplier = if (modelType == "QUALITY") 1.3f else 1.8f
        
        // 2. Compute dynamic noise reduction gains for each band
        for (b in 0..2) {
            val rms = bandRms[b]
            
            // Track the noise floor (minimum power tracker)
            if (noiseFloors[b] < 0f) {
                noiseFloors[b] = rms
            } else {
                if (rms < noiseFloors[b]) {
                    noiseFloors[b] = noiseFloors[b] * 0.4f + rms * 0.6f // Fast adaptation downward
                } else {
                    noiseFloors[b] = noiseFloors[b] * 0.999f + rms * 0.001f // Slow adaptation upward
                }
            }
            
            val noiseFloor = noiseFloors[b]
            val snr = rms / (noiseFloor + 1e-5f)
            
            // Calculate target gain using soft-knee thresholding
            var targetGain = (1f - (noiseGateMultiplier * noiseFloor / (rms + 1e-5f))).coerceIn(0.01f, 1.0f)
            
            // Suppress noise even more aggressively when SNR is low
            if (snr < 2.0f) {
                val ratio = snr / 2.0f
                targetGain *= ratio * ratio
            }
            
            // Smooth gain using envelope follower (Attack: fast, Release: slow)
            if (targetGain > currentGains[b]) {
                currentGains[b] = currentGains[b] * 0.6f + targetGain * 0.4f // Fast attack (speech onset)
            } else {
                currentGains[b] = currentGains[b] * 0.93f + targetGain * 0.07f // Slow release (speech tails)
            }
        }
        
        // 3. Reconstruct clean speech from gained bands and apply rumble/hiss filtering
        for (i in 0 until size) {
            val s1 = b1Samples[i] * currentGains[0]
            val s2 = b2Samples[i] * currentGains[1]
            val s3 = b3Samples[i] * currentGains[2]
            
            val reconstructed = s1 + s2 + s3
            
            // Filter rumble below 120Hz and hiss above 5.5kHz
            val cleanVal = outputLowpass.process(outputHighpass.process(reconstructed)).toFloat()
            
            cleanOutput[i] = cleanVal.coerceIn(-1.0f, 1.0f)
            noiseOutput[i] = (inputFrame[i] - cleanVal).coerceIn(-1.0f, 1.0f)
        }
    }
    
    // Biquad filter implementation
    private class BiquadFilter(
        val type: Type,
        val sampleRate: Double,
        val frequency: Double,
        val Q: Double
    ) {
        enum class Type { LOWPASS, HIGHPASS, BANDPASS }
        
        private var b0 = 0.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0
        
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0
        
        init {
            updateCoefficients()
        }
        
        fun updateCoefficients() {
            val w0 = 2.0 * Math.PI * frequency / sampleRate
            val alpha = Math.sin(w0) / (2.0 * Q)
            val cosW0 = Math.cos(w0)
            
            val a0: Double
            when (type) {
                Type.LOWPASS -> {
                    b0 = (1.0 - cosW0) / 2.0
                    b1 = 1.0 - cosW0
                    b2 = (1.0 - cosW0) / 2.0
                    a0 = 1.0 + alpha
                    a1 = -2.0 * cosW0
                    a2 = 1.0 - alpha
                }
                Type.HIGHPASS -> {
                    b0 = (1.0 + cosW0) / 2.0
                    b1 = -(1.0 + cosW0)
                    b2 = (1.0 + cosW0) / 2.0
                    a0 = 1.0 + alpha
                    a1 = -2.0 * cosW0
                    a2 = 1.0 - alpha
                }
                Type.BANDPASS -> {
                    b0 = alpha
                    b1 = 0.0
                    b2 = -alpha
                    a0 = 1.0 + alpha
                    a1 = -2.0 * cosW0
                    a2 = 1.0 - alpha
                }
            }
            
            b0 /= a0
            b1 /= a0
            b2 /= a0
            a1 /= a0
            a2 /= a0
        }
        
        fun process(sample: Double): Double {
            val y = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = sample
            y2 = y1
            y1 = y
            return y
        }
        
        fun reset() {
            x1 = 0.0
            x2 = 0.0
            y1 = 0.0
            y2 = 0.0
        }
    }
}
