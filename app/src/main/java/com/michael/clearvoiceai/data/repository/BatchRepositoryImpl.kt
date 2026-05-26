package com.michael.clearvoiceai.data.repository

import com.michael.clearvoiceai.data.mapper.toDomain
import com.michael.clearvoiceai.data.mapper.toEntity
import com.michael.clearvoiceai.data.source.local.BatchJobDao
import com.michael.clearvoiceai.domain.model.BatchJob
import com.michael.clearvoiceai.domain.repository.BatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BatchRepositoryImpl(
    private val batchJobDao: BatchJobDao
) : BatchRepository {

    override fun getAllJobs(): Flow<List<BatchJob>> {
        return batchJobDao.getAllJobs().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getJobById(jobId: Long): BatchJob? = withContext(Dispatchers.IO) {
        batchJobDao.getJobById(jobId)?.toDomain()
    }

    override suspend fun insertJob(job: BatchJob): Long = withContext(Dispatchers.IO) {
        batchJobDao.insertJob(job.toEntity().copy(id = 0)) // Autogenerate primary key on creation
    }

    override suspend fun updateStatus(jobId: Long, status: String): Unit = withContext(Dispatchers.IO) {
        batchJobDao.updateStatus(jobId, status)
    }

    override suspend fun updateProgress(jobId: Long, progress: Float): Unit = withContext(Dispatchers.IO) {
        batchJobDao.updateProgress(jobId, progress)
    }

    override suspend fun updateResults(jobId: Long, voiceUri: String, noiseUri: String): Unit = withContext(Dispatchers.IO) {
        batchJobDao.updateResults(jobId, voiceUri, noiseUri)
    }

    override suspend fun deleteJob(jobId: Long): Unit = withContext(Dispatchers.IO) {
        batchJobDao.deleteJob(jobId)
    }
}
