package com.michael.clearvoiceai.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchJobDao {
    @Query("SELECT * FROM batch_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<BatchJobEntity>>

    @Query("SELECT * FROM batch_jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: Long): BatchJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: BatchJobEntity): Long

    @Query("UPDATE batch_jobs SET status = :status WHERE id = :jobId")
    suspend fun updateStatus(jobId: Long, status: String)

    @Query("UPDATE batch_jobs SET progress = :progress WHERE id = :jobId")
    suspend fun updateProgress(jobId: Long, progress: Float)

    @Query("UPDATE batch_jobs SET voiceResultUri = :voiceUri, noiseResultUri = :noiseUri WHERE id = :jobId")
    suspend fun updateResults(jobId: Long, voiceUri: String, noiseUri: String)

    @Query("DELETE FROM batch_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: Long)
}
