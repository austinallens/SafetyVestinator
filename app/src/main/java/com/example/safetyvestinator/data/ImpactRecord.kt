package com.example.safetyvestinator.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "impacts")
data class ImpactRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val locationAgeMillis: Long?,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val gx: Float,
    val gy: Float,
    val gz: Float,
    val tempF: Float
)