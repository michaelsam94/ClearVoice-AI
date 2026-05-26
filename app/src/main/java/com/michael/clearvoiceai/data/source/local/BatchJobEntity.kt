package com.michael.clearvoiceai.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_jobs")
data class BatchJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inputUri: String,
    val status: String,
    val progress: Float,
    val voiceResultUri: String?,
    val noiseResultUri: String?,
    val createdAt: Long = System.currentTimeMillis()
)
