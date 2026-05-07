package com.example.safetyvestinator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safetyvestinator.ui.theme.SafetyVestinatorTheme
import com.example.safetyvestinator.viewmodel.BleViewModel
import com.example.safetyvestinator.viewmodel.SettingsViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.safetyvestinator.data.NotificationHelper
import org.osmdroid.config.Configuration
import androidx.preference.PreferenceManager
import com.example.safetyvestinator.data.ConnectionState
import com.example.safetyvestinator.data.EmailSender
import com.example.safetyvestinator.data.ImpactRepository
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For osmdroid Map Support
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val bleViewModel: BleViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SafetyVestinatorTheme(darkTheme = isDark) {
                SafetyVestinatorApp(
                    settingsViewModel = settingsViewModel,
                    bleViewModel = bleViewModel
                )
            }
        }
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun SafetyVestinatorApp(
    settingsViewModel: SettingsViewModel,
    bleViewModel: BleViewModel
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val debugMode by settingsViewModel.debugMode.collectAsStateWithLifecycle()
    val connectionState by bleViewModel.state.collectAsStateWithLifecycle()

    // Push Debug mode to ESP whenever changes
    LaunchedEffect(debugMode, connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            bleViewModel.setDebugMode(debugMode)
        }
    }

    val context = LocalContext.current

    val impactRepo = remember { ImpactRepository(context) }

    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannel(context)
        bleViewModel.impacts.collect { timestamp ->
            // Record to Database First
            impactRepo.recordImpact(
                timestamp = timestamp,
                location = bleViewModel.location.value,
                latestReading = bleViewModel.recentReadings.value.lastOrNull()
            )

            Log.d("MainActivity", "Impact event received")
            NotificationHelper.showImpact(context)
            EmailSender.sendImpactAlert(
                recipientEmail = settingsViewModel.recipientEmail.value,
                location = bleViewModel.location.value
            )
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = screenModifier,
                    bleViewModel = bleViewModel,
                    debugMode = debugMode
                )
                AppDestinations.CALENDAR -> CalendarScreen(
                    modifier = screenModifier,
                    impactRepo = impactRepo
                )
                AppDestinations.SETTINGS -> SettingsScreen(
                    modifier = screenModifier,
                    settingsViewModel = settingsViewModel,
                    bleViewModel = bleViewModel
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    CALENDAR("Calendar", R.drawable.ic_calendar),
    HOME("Home", R.drawable.ic_home),
    SETTINGS("Settings", R.drawable.ic_account_box),
}