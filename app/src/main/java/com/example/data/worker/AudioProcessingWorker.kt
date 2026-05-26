package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.di.DependencyProvider
import com.example.domain.model.BatchStatus
import kotlinx.coroutines.CancellationException

class AudioProcessingWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        const val KEY_JOB_ID = "KEY_JOB_ID"
        const val KEY_URI = "KEY_URI"
        const val KEY_MODEL = "KEY_MODEL"
        const val KEY_FORMAT = "KEY_FORMAT"
        
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val CHANNEL_ID = "clearvoice_channel"
        const val NOTIFICATION_ID = 82300
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val jobId = inputData.getLong(KEY_JOB_ID, -1L)
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val modelType = inputData.getString(KEY_MODEL) ?: "FAST"
        val exportFormat = inputData.getString(KEY_FORMAT) ?: "WAV"
        
        val batchRepository = DependencyProvider.getBatchRepository(context)
        val processAudioUseCase = DependencyProvider.getProcessAudioUseCase(context)

        createNotificationChannel()
        setForeground(getForegroundInfo(jobId, 0f))

        return try {
            batchRepository.updateStatus(jobId, BatchStatus.PROCESSING.name)

            val stems = processAudioUseCase(uriStr, modelType, exportFormat) { progress ->
                kotlinx.coroutines.runBlocking {
                    batchRepository.updateProgress(jobId, progress * 100f)
                    setProgress(workDataOf(KEY_PROGRESS to (progress * 100f)))
                    try {
                        setForeground(getForegroundInfo(jobId, progress))
                    } catch (e: Exception) {
                        // Suppress background thread transition flags
                    }
                }
            }

            batchRepository.updateResults(jobId, stems.first, stems.second)
            batchRepository.updateStatus(jobId, BatchStatus.DONE.name)
            batchRepository.updateProgress(jobId, 100f)
            
            Result.success()
        } catch (e: CancellationException) {
            batchRepository.updateStatus(jobId, BatchStatus.CANCELLED.name)
            Result.failure()
        } catch (e: Exception) {
            Log.e("AudioProcessingWorker", "Error processing batch job $jobId: ${e.message}", e)
            batchRepository.updateStatus(jobId, BatchStatus.FAILED.name)
            Result.failure()
        }
    }

    private fun getForegroundInfo(jobId: Long, progress: Float): ForegroundInfo {
        val pct = (progress * 100).toInt()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("ClearVoice AI")
            .setContentText("Suppressing audio noise... $pct%")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setProgress(100, pct, false)
            .setOngoing(true)
            .build()
            
        return ForegroundInfo(NOTIFICATION_ID + jobId.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ClearVoice AI Processing"
            val descriptionText = "Shows progress of offline audio filters"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
