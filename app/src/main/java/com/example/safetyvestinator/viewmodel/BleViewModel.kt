package com.example.safetyvestinator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.safetyvestinator.data.BleManager

class BleViewModel(application: Application) : AndroidViewModel(application) {

    private val ble = BleManager(application)

    val state = ble.state
    val reading = ble.reading
    val impacts = ble.impacts
    val recentReadings = ble.recentReadings

    fun connect() = ble.startScan()
    fun disconnect() = ble.disconnect()

    override fun onCleared() {
        super.onCleared()
        ble.disconnect()
    }
}