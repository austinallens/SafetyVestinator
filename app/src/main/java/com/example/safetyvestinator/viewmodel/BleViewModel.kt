package com.example.safetyvestinator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.safetyvestinator.data.BleManager
import com.example.safetyvestinator.data.PhoneLocationProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.safetyvestinator.data.GpsLocation
import androidx.lifecycle.viewModelScope

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val ble = BleManager(application)

    val state = ble.state
    val impacts = ble.impacts
    val recentReadings = ble.recentReadings
    fun connect() = ble.startScan()
    fun disconnect() = ble.disconnect()
    fun setDebugMode(enabled: Boolean) = ble.setDebugMode(enabled)
    fun fireTestImpact() = ble.fireTestImpact()

    override fun onCleared() {
        super.onCleared()
        ble.disconnect()
    }

    private val phoneLocationProvider = PhoneLocationProvider(application)

    private val phoneLocation: StateFlow<GpsLocation?> = phoneLocationProvider.locationFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val location: StateFlow<GpsLocation?> = combine(
        ble.location,
        phoneLocation
    ) { vest, phone ->
        pickFreshest(vest, phone)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private fun pickFreshest(vest: GpsLocation?, phone: GpsLocation?): GpsLocation? {
        if (vest == null) return phone
        if (phone == null) return vest
        return if (vest.receivedAtMillis >= phone.receivedAtMillis) vest else phone
    }
}