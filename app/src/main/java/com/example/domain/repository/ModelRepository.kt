package com.example.domain.repository

interface ModelRepository {
    suspend fun runInference(
        inputFrame: FloatArray,
        cleanOutput: FloatArray,
        noiseOutput: FloatArray,
        modelType: String
    )
}
