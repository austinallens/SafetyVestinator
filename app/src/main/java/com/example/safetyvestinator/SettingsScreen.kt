package com.example.safetyvestinator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safetyvestinator.data.ConnectionState
import com.example.safetyvestinator.ui.rememberBlePermissionsState
import com.example.safetyvestinator.viewmodel.BleViewModel
import com.example.safetyvestinator.viewmodel.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    bleViewModel: BleViewModel
    ) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val deviceName by settingsViewModel.deviceName.collectAsStateWithLifecycle()
    val connectionState by bleViewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberBlePermissionsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Appearance") {
            ThemeSelector(
                selected = themeMode,
                onSelected = settingsViewModel::setThemeMode
            )
        }

        SettingsSection(title = "Device") {
            ConnectionStatus(state = connectionState)
            Button(
                onClick = {
                    when (connectionState) {
                        ConnectionState.CONNECTED, ConnectionState.SCANNING, ConnectionState.CONNECTING ->
                            bleViewModel.disconnect()
                        ConnectionState.DISCONNECTED -> {
                            if (permissions.allPermissionsGranted) {
                                bleViewModel.connect()
                            } else {
                                permissions.launchMultiplePermissionRequest()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (connectionState) {
                        ConnectionState.CONNECTED -> "Disconnect"
                        ConnectionState.SCANNING -> "Scanning... (tap to cancel)"
                        ConnectionState.CONNECTING -> "Connecting... (tap to cancel)"
                        ConnectionState.DISCONNECTED -> "Pair Device"
                    }
                )
            }
            OutlinedTextField(
                value = deviceName,
                onValueChange = settingsViewModel::setDeviceName,
                label = { Text("Device Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit
) {
    val options = listOf(
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark",
        ThemeMode.SYSTEM to "System"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                )
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    val connected = state == ConnectionState.CONNECTED
    val color = if (connected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = when (state) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.SCANNING -> "Scanning..."
        ConnectionState.DISCONNECTED -> "Not Paired"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (connected) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
            contentDescription = null,
            tint = color
        )
        Text(text = text, color = color)
    }
}