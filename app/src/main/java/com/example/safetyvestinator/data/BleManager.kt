package com.example.safetyvestinator.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

private val pendingNotificationSetups = mutableListOf<BluetoothGattCharacteristic>()
private val SERVICE_UUID    = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
private val SENSOR_CHAR     = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
private val IMPACT_CHAR     = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
private val GPS_CHAR = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
private val CONFIG_CHAR = UUID.fromString("12345678-1234-5678-1234-56789abcdef4")
private val CCCD_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private var lastImpactMillis: Long = 0L
private val impactDebounceMillis = 5_000L

data class SensorReading(
    val ax: Float, val ay: Float, val az: Float,
    val gx: Float, val gy: Float, val gz: Float,
    val tempF: Float
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val receivedAtMillis: Long
)

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var gatt: BluetoothGatt? = null

    private var configCharacteristic: BluetoothGattCharacteristic? = null

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state = _state.asStateFlow()

    private val _impacts = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val impacts = _impacts.asSharedFlow()

    private val _recentReadings = MutableStateFlow<List<SensorReading>>(emptyList())
    val recentReadings = _recentReadings.asStateFlow()

    private val maxBufferSize = 100 // About 10 seconds at 10 Hertz

    private val _location = MutableStateFlow<GpsLocation?>(null)
    val location = _location.asStateFlow()

    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        if (_state.value != ConnectionState.DISCONNECTED) return
        _state.value = ConnectionState.SCANNING

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun disconnect() {
        gatt?.disconnect()
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_state.value == ConnectionState.SCANNING) {
            _state.value = ConnectionState.DISCONNECTED
        }
    }

    fun setDebugMode(enabled: Boolean) {
        val ch = configCharacteristic ?: return
        writeCharacteristicCompat(ch, byteArrayOf(if (enabled) 0x01 else 0x00))
    }

    fun fireTestImpact() {
        val ch = configCharacteristic ?: run {
            Log.e("BleManager", "fireTestImpact: configCharacteristic is null")
            return
        }
        Log.d("BleManager", "fireTestImpact: writing 0xFF")
        writeCharacteristicCompat(ch, byteArrayOf(0xFF.toByte()))
    }

    private fun writeCharacteristicCompat(ch: BluetoothGattCharacteristic, data: ByteArray) {
        val g = gatt ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(ch, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            ch.value = data
            @Suppress("DEPRECATION")
            g.writeCharacteristic(ch)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            adapter?.bluetoothLeScanner?.stopScan(this)
            connect(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = ConnectionState.DISCONNECTED
        }
    }

    private fun connect(device: BluetoothDevice) {
        _state.value = ConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _state.value = ConnectionState.CONNECTED
                    g.requestMtu(64)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _state.value = ConnectionState.DISCONNECTED
                    g.close()
                    gatt = null
                    configCharacteristic = null
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(SERVICE_UUID) ?: return

            configCharacteristic = service.getCharacteristic(CONFIG_CHAR)

            pendingNotificationSetups.clear()
            listOf(SENSOR_CHAR, IMPACT_CHAR, GPS_CHAR).forEach { uuid ->
                service.getCharacteristic(uuid)?.let { pendingNotificationSetups.add(it) }
            }
            processNextNotificationSetup(g)
        }

        private fun processNextNotificationSetup(g: BluetoothGatt) {
            if (pendingNotificationSetups.isEmpty()) return
            val ch = pendingNotificationSetups.removeAt(0)
            enableNotifications(g, ch)
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d("BleManager", "onDescriptorWrite: status=$status uuid=${descriptor.characteristic.uuid}")
            processNextNotificationSetup(g)
        }

        // API 33+ signature
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            ch: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d("BleManager", "onCharacteristicChanged (API 33+): uuid=${ch.uuid}, ${value.size} bytes")
            handleCharacteristicValue(ch.uuid, value)
        }

        // Pre-API 33 signature
        @Deprecated("Used on API < 33", ReplaceWith(""))
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            ch: BluetoothGattCharacteristic
        ) {
            Log.d("BleManager", "onCharacteristicChanged (pre-33): uuid=${ch.uuid}")
            @Suppress("DEPRECATION")
            handleCharacteristicValue(ch.uuid, ch.value)
        }
    }

    private fun handleCharacteristicValue(uuid: UUID, value: ByteArray) {
        Log.d("BleManager", "handleCharacteristicValue: $uuid")
        when (uuid) {
            SENSOR_CHAR -> parseSensor(value)?.let { reading ->
                _recentReadings.update { current ->
                    (current + reading).takeLast(maxBufferSize)
                }
            }
            IMPACT_CHAR -> {
                val now = System.currentTimeMillis()
                if (now - lastImpactMillis >= impactDebounceMillis) {
                    lastImpactMillis = now
                    _impacts.tryEmit(now)
                    Log.d("BleManager", "Impact accepted")
                } else {
                    Log.d("BleManager", "Impact suppressed (within ${impactDebounceMillis}ms cooldown)")
                }
            }
            GPS_CHAR -> parseGps(value)?.let { _location.value = it }
        }
    }

    private fun enableNotifications(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(ch, true)
        val descriptor = ch.getDescriptor(CCCD_DESCRIPTOR) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            g.writeDescriptor(descriptor)
        }
    }

    private fun parseSensor(data: ByteArray): SensorReading? {
        if (data.size < 28) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        return SensorReading(
            ax = buf.float, ay = buf.float, az = buf.float,
            gx = buf.float, gy = buf.float, gz = buf.float,
            tempF = buf.float
        )
    }

    private fun parseGps(data: ByteArray): GpsLocation? {
        if (data.size < 8) return null
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val lat = buf.float.toDouble()
        val lng = buf.float.toDouble()
        return GpsLocation(lat, lng, System.currentTimeMillis())
    }
}