package com.example.domain.repository

interface AudioRepository {
    suspend fun decodeToFrames(inputUriStr: String, onProgress: (Float) -> Unit): List<FloatArray>
    suspend fun saveStems(
        originalName: String,
        cleanFrames: List<FloatArray>,
        noiseFrames: List<FloatArray>,
        format: String
    ): Pair<String, String>
}
