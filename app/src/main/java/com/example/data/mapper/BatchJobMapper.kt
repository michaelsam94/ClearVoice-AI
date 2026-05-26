package com.example.data.mapper

import com.example.data.source.local.BatchJobEntity
import com.example.domain.model.BatchJob
import com.example.domain.model.BatchStatus

fun BatchJobEntity.toDomain(): BatchJob {
    val domainStatus = try {
        BatchStatus.valueOf(this.status)
    } catch (e: Exception) {
        BatchStatus.QUEUED
    }
    
    return BatchJob(
        id = this.id,
        name = this.name,
        inputUri = this.inputUri,
        status = domainStatus,
        progress = this.progress,
        voiceResultUri = this.voiceResultUri,
        noiseResultUri = this.noiseResultUri,
        createdAt = this.createdAt
    )
}

fun BatchJob.toEntity(): BatchJobEntity {
    return BatchJobEntity(
        id = this.id,
        name = this.name,
        inputUri = this.inputUri,
        status = this.status.name,
        progress = this.progress,
        voiceResultUri = this.voiceResultUri,
        noiseResultUri = this.noiseResultUri,
        createdAt = this.createdAt
    )
}
