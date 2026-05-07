package com.example.safetyvestinator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImpactDao {

    @Insert
    suspend fun insert(record: ImpactRecord): Long

    @Query("SELECT * FROM impacts WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC")
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<ImpactRecord>>

    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch', 'localtime') FROM impacts")
    fun observeDaysWithImpacts(): Flow<List<String>>
}