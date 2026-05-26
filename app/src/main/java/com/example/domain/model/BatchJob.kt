package com.example.domain.model

enum class BatchStatus {
    QUEUED, PROCESSING, DONE, FAILED, CANCELLED
}

data class BatchJob(
    val id: Long,
    val name: String,
    val inputUri: String,
    val status: BatchStatus,
    val progress: Float,
    val voiceResultUri: String?,
    val noiseResultUri: String?,
    val createdAt: Long
)
