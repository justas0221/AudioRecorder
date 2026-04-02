package com.example.audiorecorder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords ORDER BY timestamp DESC")
    fun getAll(): List<AudioRecord>

    @Query("SELECT * FROM audioRecords WHERE fileName LIKE :query")
    fun searchDatabase(query: String): List<AudioRecord>

    @Query("SELECT COUNT(*) FROM audioRecords WHERE fileName = :name")
    fun countByName(name: String): Int

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: List<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)

    @Query("DELETE FROM audioRecords")
    fun deleteAll()
}