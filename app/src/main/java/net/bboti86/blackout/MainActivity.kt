package net.bboti86.blackout

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val BATTERY_THRESHOLD_ENABLED = booleanPreferencesKey("battery_threshold_enabled")
        val BATTERY_THRESHOLD_VALUE = intPreferencesKey("battery_threshold_value")
        val TIME_TRIGGER_ENABLED = booleanPreferencesKey("time_trigger_enabled")
        val TIME_TRIGGER_VALUE = stringPreferencesKey("time_trigger_value") // HH:mm
    }

    val batteryThresholdEnabled: Flow<Boolean> = context.dataStore.data.map { it[BATTERY_THRESHOLD_ENABLED] ?: false }
    val batteryThresholdValue: Flow<Int> = context.dataStore.data.map { it[BATTERY_THRESHOLD_VALUE] ?: 20 }
    val timeTriggerEnabled: Flow<Boolean> = context.dataStore.data.map { it[TIME_TRIGGER_ENABLED] ?: false }
    val timeTriggerValue: Flow<String> = context.dataStore.data.map { it[TIME_TRIGGER_VALUE] ?: "00:00" }

    suspend fun saveSettings(
        batteryEnabled: Boolean,
        batteryValue: Int,
        timeEnabled: Boolean,
        timeValue: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[BATTERY_THRESHOLD_ENABLED] = batteryEnabled
            preferences[BATTERY_THRESHOLD_VALUE] = batteryValue
            preferences[TIME_TRIGGER_ENABLED] = timeEnabled
            preferences[TIME_TRIGGER_VALUE] = timeValue
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Reroute to secondary display if launched on default display
        if (rerouteToSecondaryDisplay()) return

        // Enable immersive full-screen mode to hide status bar & navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            BlackoutScreen { finishAffinity() }
        }
    }
}

@Composable
fun BlackoutScreen(onExitApp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // Settings state
    val batteryEnabled by settingsManager.batteryThresholdEnabled.collectAsState(initial = false)
    val batteryThreshold by settingsManager.batteryThresholdValue.collectAsState(initial = 20)
    val timeEnabled by settingsManager.timeTriggerEnabled.collectAsState(initial = false)
    val triggerTime by settingsManager.timeTriggerValue.collectAsState(initial = "00:00")

    var showSettings by remember { mutableStateOf(false) }

    // Visibility state for the overlay
    var isVisible by remember { mutableStateOf(value = false) }

    // Trigger key to restart the 10-second timer whenever touched
    var touchCount by remember { mutableIntStateOf(0) }

    // Current time and battery state
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var batteryPercentage by remember { mutableIntStateOf(getBatteryLevel(context)) }

    // Pulsing color for low battery (< 15%)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseColor by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = if (batteryPercentage < 15) Color.Red else Color.White,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseColor"
    )

    // System Broadcasts (Time and Battery)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if ((level >= 0) && (scale > 0)) {
                            batteryPercentage = (level * 100) / scale
                        }
                    }
                    Intent.ACTION_TIME_TICK -> {
                        // Efficient minute-by-minute update
                        if (!isVisible) {
                            currentTime = getCurrentTime()
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Trigger logic for battery and time thresholds
    // Time trigger lasts for 1 hour
    val isThresholdActive = remember(batteryPercentage, currentTime, batteryEnabled, batteryThreshold, timeEnabled, triggerTime) {
        val bTrigger = batteryEnabled && batteryPercentage <= batteryThreshold
        val tTrigger = if (timeEnabled) {
            val nowMinutes = timeToMinutes(currentTime)
            val triggerMinutes = timeToMinutes(triggerTime)
            // Check if current time is within [triggerTime, triggerTime + 60 minutes)
            val diff = (nowMinutes - triggerMinutes + 1440) % 1440
            diff < 60
        } else false
        bTrigger || tTrigger
    }

    LaunchedEffect(isThresholdActive) {
        if (isThresholdActive) {
            isVisible = true
        }
    }

    // 10-second countdown logic with live time updates
    LaunchedEffect(touchCount) {
        if (touchCount > 0) {
            isVisible = true
            currentTime = getCurrentTime()
            batteryPercentage = getBatteryLevel(context)

            // Keep time updated every second for 10 seconds total
            repeat(10) {
                delay(1.seconds)
                currentTime = getCurrentTime()
            }
            // Only hide if we aren't triggered by thresholds
            if (!isThresholdActive) {
                isVisible = false
            }
        }
    }

    // Constant second-by-second update ONLY when visible
    LaunchedEffect(isVisible) {
        while (isVisible) {
            delay(1.seconds)
            currentTime = getCurrentTime()
        }
    }

    // Root full-screen container (Pure OLED Black)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No visual ripple on tap
            ) {
                touchCount++
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Settings button at top right
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                ) {
                    // Clock display
                    Text(
                        text = currentTime,
                        color = pulseColor,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Battery level
                    Text(
                        text = "$batteryPercentage%",
                        color = if (batteryPercentage < 15) pulseColor else Color(0xFFAAAAAA),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Exit App Button
                    Button(
                        onClick = onExitApp,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF222222),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Exit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialBatteryEnabled = batteryEnabled,
            initialBatteryThreshold = batteryThreshold,
            initialTimeEnabled = timeEnabled,
            initialTriggerTime = triggerTime,
            onDismiss = { showSettings = false },
            onSave = { bE, bT, tE, tT ->
                scope.launch {
                    settingsManager.saveSettings(bE, bT, tE, tT)
                }
                showSettings = false
            }
        )
    }
}

@Composable
fun SettingsDialog(
    initialBatteryEnabled: Boolean,
    initialBatteryThreshold: Int,
    initialTimeEnabled: Boolean,
    initialTriggerTime: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, Boolean, String) -> Unit
) {
    var batteryEnabled by remember { mutableStateOf(initialBatteryEnabled) }
    var batteryThreshold by remember { mutableIntStateOf(initialBatteryThreshold) }
    var timeEnabled by remember { mutableStateOf(initialTimeEnabled) }
    var triggerTime by remember { mutableStateOf(initialTriggerTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                // Battery Threshold
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Battery Threshold", modifier = Modifier.weight(1f))
                    Switch(checked = batteryEnabled, onCheckedChange = { batteryEnabled = it })
                }
                if (batteryEnabled) {
                    Slider(
                        value = batteryThreshold.toFloat(),
                        onValueChange = { batteryThreshold = it.toInt() },
                        valueRange = 0f..100f
                    )
                    Text("Trigger at $batteryThreshold%")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Trigger
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time Trigger", modifier = Modifier.weight(1f))
                    Switch(checked = timeEnabled, onCheckedChange = { timeEnabled = it })
                }
                if (timeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactTimePicker(
                        time = triggerTime,
                        onTimeChange = { triggerTime = it }
                    )
                    Text("Active for 1 hour from $triggerTime", fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(batteryEnabled, batteryThreshold, timeEnabled, triggerTime) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompactTimePicker(
    time: String,
    onTimeChange: (String) -> Unit
) {
    val hour = time.split(":")[0].toIntOrNull() ?: 0
    val minute = time.split(":")[1].toIntOrNull() ?: 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TimeColumn(value = hour, label = "Hour", range = 0..23) {
            onTimeChange(String.format(Locale.getDefault(), "%02d:%02d", it, minute))
        }
        Text(":", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
        TimeColumn(value = minute, label = "Min", range = 0..59) {
            onTimeChange(String.format(Locale.getDefault(), "%02d:%02d", hour, it))
        }
    }
}

@Composable
fun TimeColumn(
    value: Int,
    label: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatingIconButton(onClick = {
            val next = if (value + 1 > range.last) range.first else value + 1
            onValueChange(next)
        }) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label")
        }
        Text(String.format(Locale.getDefault(), "%02d", value), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        RepeatingIconButton(onClick = {
            val prev = if (value - 1 < range.first) range.last else value - 1
            onValueChange(prev)
        }) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
        }
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick() // First click immediate
            delay(500.milliseconds) // Initial wait
            while (isPressed) {
                onClick()
                delay(100.milliseconds) // Repeat speed
            }
        }
    }

    IconButton(
        onClick = {}, // Logic handled by isPressed LaunchedEffect
        interactionSource = interactionSource,
        content = content
    )
}

// Helper function to format system time (e.g. 14:05)
private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}

// Helper function to read immediate battery level on touch
private fun getBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
}

private fun ComponentActivity.rerouteToSecondaryDisplay(): Boolean {
    val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.displayId
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.displayId
    }

    if (currentDisplayId == Display.DEFAULT_DISPLAY) {
        val secondaryDisplay = displayManager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (secondaryDisplay != null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = secondaryDisplay.displayId
            startActivity(intent, options.toBundle())
            finish()
            return true
        }
    }
    return false
}