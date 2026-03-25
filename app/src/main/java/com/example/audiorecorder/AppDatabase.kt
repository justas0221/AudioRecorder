package com.example.audiorecorder

import android.provider.MediaStore
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AudioRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioRecordDao() : AudioRecordDao
}