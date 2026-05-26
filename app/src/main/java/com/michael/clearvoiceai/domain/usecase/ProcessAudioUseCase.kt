package com.michael.clearvoiceai.domain.usecase

import com.michael.clearvoiceai.domain.repository.AudioRepository
import com.michael.clearvoiceai.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProcessAudioUseCase(
    private val audioRepository: AudioRepository,
    private val modelRepository: ModelRepository
) {
    suspend operator fun invoke(
        inputUriStr: String,
        modelType: String,
        exportFormat: String,
        onProgress: (Float) -> Unit
    ): Pair<String, String> = withContext(Dispatchers.Default) {
        modelRepository.reset()
        // Decode original audio to float frames
        val frames = audioRepository.decodeToFrames(inputUriStr) { progress ->
            onProgress(progress * 0.3f)
        }
        
        if (frames.isEmpty()) {
            throw IllegalArgumentException("Could not read sound channels or parse stream headers.")
        }

        val cleanFrames = ArrayList<FloatArray>(frames.size)
        val noiseFrames = ArrayList<FloatArray>(frames.size)
        
        val totalFrames = frames.size
        for (i in 0 until totalFrames) {
            val inputFrame = frames[i]
            val cleanOutput = FloatArray(inputFrame.size)
            val noiseOutput = FloatArray(inputFrame.size)
            
            // Run LiteRT inference or high-fidelity offline DSP filters
            modelRepository.runInference(inputFrame, cleanOutput, noiseOutput, modelType)
            
            cleanFrames.add(cleanOutput)
            noiseFrames.add(noiseOutput)
            
            if (i % 20 == 0 || i == totalFrames - 1) {
                val modelProgress = 0.3f + ((i.toFloat() / totalFrames) * 0.5f)
                onProgress(modelProgress)
            }
        }
        
        // Extract original file name
        val rawName = try {
            val lastSegment = inputUriStr.substringAfterLast("/")
            val cleanPart = lastSegment.substringBefore("?")
            if (cleanPart.contains(".")) cleanPart.substringBeforeLast(".") else "audio_file"
        } catch (e: Exception) {
            "audio_file"
        }
        
        onProgress(0.85f)
        val stems = audioRepository.saveStems(rawName, cleanFrames, noiseFrames, exportFormat)
        onProgress(1.0f)
        
        stems
    }
}
