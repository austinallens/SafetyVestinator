package com.example.safetyvestinator.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@SuppressLint("MissingPermission")
class PhoneLocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun locationFlow(): Flow<GpsLocation> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(GpsLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        receivedAtMillis = System.currentTimeMillis()
                    ))
                }
            }
        }

        Log.d("PhoneLocation", "Starting phone location updates")
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            Log.d("PhoneLocation", "Stopping phone location updates")
            client.removeLocationUpdates(callback)
        }
    }
}