package com.michael.clearvoiceai.domain.repository

import com.michael.clearvoiceai.domain.model.BatchJob
import kotlinx.coroutines.flow.Flow

interface BatchRepository {
    fun getAllJobs(): Flow<List<BatchJob>>
    suspend fun getJobById(jobId: Long): BatchJob?
    suspend fun insertJob(job: BatchJob): Long
    suspend fun updateStatus(jobId: Long, status: String)
    suspend fun updateProgress(jobId: Long, progress: Float)
    suspend fun updateResults(jobId: Long, voiceResultUri: String, noiseResultUri: String)
    suspend fun deleteJob(jobId: Long)
}
