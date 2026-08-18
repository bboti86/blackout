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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Visibility state for the overlay
    var isVisible by remember { mutableStateOf(value = false) }

    // Trigger key to restart the 10-second timer whenever touched
    var touchCount by remember { mutableIntStateOf(0) }

    // Current time and battery state
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var batteryPercentage by remember { mutableIntStateOf(getBatteryLevel(context)) }

    // Battery status broadcast receiver
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if ((level >= 0) && (scale > 0)) {
                        batteryPercentage = (level * 100) / scale
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
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
            isVisible = false
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Clock display
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Battery level
                Text(
                    text = "$batteryPercentage%",
                    color = Color(0xFFAAAAAA),
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

// Helper function to format system time (e.g. 14:05)
private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
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