package com.example.safetyvestinator.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class ImpactRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).impactDao()

    suspend fun recordImpact(
        timestamp: Long,
        location: GpsLocation?,
        latestReading: SensorReading?
    ) {
        if (latestReading == null) return  // can't record without sensor data
        val locationAge = location?.let { timestamp - it.receivedAtMillis }
        val record = ImpactRecord(
            timestamp = timestamp,
            latitude = location?.latitude,
            longitude = location?.longitude,
            locationAgeMillis = locationAge,
            ax = latestReading.ax,
            ay = latestReading.ay,
            az = latestReading.az,
            gx = latestReading.gx,
            gy = latestReading.gy,
            gz = latestReading.gz,
            tempF = latestReading.tempF
        )
        dao.insert(record)
    }

    fun observeImpactsForDay(date: LocalDate): Flow<List<ImpactRecord>> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.observeBetween(startMillis, endMillis)
    }

    fun observeDaysWithImpacts(): Flow<List<LocalDate>> =
        dao.observeDaysWithImpacts().map { dates ->
            dates.mapNotNull { dateString ->
                try { LocalDate.parse(dateString) } catch (e: Exception) { null }
            }
        }
}