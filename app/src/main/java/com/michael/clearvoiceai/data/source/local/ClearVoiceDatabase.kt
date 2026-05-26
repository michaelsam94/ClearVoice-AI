package com.michael.clearvoiceai.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BatchJobEntity::class], version = 1, exportSchema = false)
abstract class ClearVoiceDatabase : RoomDatabase() {
    abstract fun batchJobDao(): BatchJobDao
}
