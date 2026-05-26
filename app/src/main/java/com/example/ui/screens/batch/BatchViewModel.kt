package com.example.ui.screens.batch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.data.source.local.ClearVoicePrefs
import com.example.data.worker.AudioProcessingWorker
import com.example.di.DependencyProvider
import com.example.domain.model.BatchJob
import com.example.domain.model.BatchStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BatchViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val batchRepository = DependencyProvider.getBatchRepository(application)
    private val prefs = ClearVoicePrefs(application)
    private val workManager = WorkManager.getInstance(context)

    val batchJobs: StateFlow<List<BatchJob>> = batchRepository.getAllJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addFilesToBatch(uris: List<String>) {
        viewModelScope.launch {
            for (uriStr in uris) {
                val rawName = try {
                    val decoded = Uri.decode(uriStr)
                    val segment = decoded.substringAfterLast("/")
                    val stem = segment.substringBefore("?")
                    if (stem.contains(".")) stem else "track_isolated.wav"
                } catch (e: Exception) {
                    "track_isolated.wav"
                }

                val job = BatchJob(
                    id = 0,
                    name = rawName,
                    inputUri = uriStr,
                    status = BatchStatus.QUEUED,
                    progress = 0f,
                    voiceResultUri = null,
                    noiseResultUri = null,
                    createdAt = System.currentTimeMillis()
                )

                val jobId = batchRepository.insertJob(job)
                enqueueWorkManagerTask(jobId, uriStr)
            }
        }
    }

    private fun enqueueWorkManagerTask(jobId: Long, uriStr: String) {
        val model = prefs.modelType
        val format = prefs.exportFormat

        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            AudioProcessingWorker.KEY_JOB_ID to jobId,
            AudioProcessingWorker.KEY_URI to uriStr,
            AudioProcessingWorker.KEY_MODEL to model,
            AudioProcessingWorker.KEY_FORMAT to format
        )

        val workRequest = OneTimeWorkRequestBuilder<AudioProcessingWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("ClearVoiceBatchJob")
            .build()

        workManager.enqueueUniqueWork(
            "ClearVoiceJob_${jobId}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            workManager.cancelUniqueWork("ClearVoiceJob_${jobId}")
            batchRepository.deleteJob(jobId)
        }
    }
}
