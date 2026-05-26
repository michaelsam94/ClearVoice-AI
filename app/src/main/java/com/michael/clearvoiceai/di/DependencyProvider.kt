package com.michael.clearvoiceai.di

import android.content.Context
import androidx.room.Room
import com.michael.clearvoiceai.data.repository.AudioRepositoryImpl
import com.michael.clearvoiceai.data.repository.BatchRepositoryImpl
import com.michael.clearvoiceai.data.repository.ModelRepositoryImpl
import com.michael.clearvoiceai.data.source.local.ClearVoiceDatabase
import com.michael.clearvoiceai.domain.repository.AudioRepository
import com.michael.clearvoiceai.domain.repository.BatchRepository
import com.michael.clearvoiceai.domain.repository.ModelRepository
import com.michael.clearvoiceai.domain.usecase.ProcessAudioUseCase

object DependencyProvider {
    private var database: ClearVoiceDatabase? = null
    private var audioRepository: AudioRepository? = null
    private var modelRepository: ModelRepository? = null
    private var batchRepository: BatchRepository? = null
    private var processAudioUseCase: ProcessAudioUseCase? = null

    fun getDatabase(context: Context): ClearVoiceDatabase {
        return database ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                ClearVoiceDatabase::class.java,
                "clearvoice_db"
            ).fallbackToDestructiveMigration().build()
            database = db
            db
        }
    }

    fun getAudioRepository(context: Context): AudioRepository {
        return audioRepository ?: synchronized(this) {
            val repo = AudioRepositoryImpl(context.applicationContext)
            audioRepository = repo
            repo
        }
    }

    fun getModelRepository(context: Context): ModelRepository {
        return modelRepository ?: synchronized(this) {
            val repo = ModelRepositoryImpl(context.applicationContext)
            modelRepository = repo
            repo
        }
    }

    fun setModelRepository(repo: ModelRepository?) {
        synchronized(this) {
            modelRepository = repo
        }
    }

    fun getBatchRepository(context: Context): BatchRepository {
        return batchRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = BatchRepositoryImpl(db.batchJobDao())
            batchRepository = repo
            repo
        }
    }

    fun getProcessAudioUseCase(context: Context): ProcessAudioUseCase {
        return processAudioUseCase ?: synchronized(this) {
            val useCase = ProcessAudioUseCase(
                getAudioRepository(context),
                getModelRepository(context)
            )
            processAudioUseCase = useCase
            useCase
        }
    }

    fun setDatabase(db: ClearVoiceDatabase?) {
        synchronized(this) { database = db }
    }

    fun setAudioRepository(repo: AudioRepository?) {
        synchronized(this) { audioRepository = repo }
    }

    fun setBatchRepository(repo: BatchRepository?) {
        synchronized(this) { batchRepository = repo }
    }

    fun setProcessAudioUseCase(useCase: ProcessAudioUseCase?) {
        synchronized(this) { processAudioUseCase = useCase }
    }
}
