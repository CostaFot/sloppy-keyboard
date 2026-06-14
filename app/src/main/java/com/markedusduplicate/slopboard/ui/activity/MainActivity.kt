package com.markedusduplicate.slopboard.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.accessibility.SlopboardAccessibilityService
import com.markedusduplicate.slopboard.clippy.ClippyOverlayService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface {
                    SetupScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        logDebug { "onDestroy" }
        super.onDestroy()
    }
}

@Composable
private fun SetupScreen() {
    val context = LocalContext.current

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var canDrawOverlays by remember { mutableStateOf(false) }
    var isClippyRunning by remember { mutableStateOf(false) }

    // Re-read status every time the Activity resumes, so returning from system
    // settings refreshes the indicators.
    LifecycleResumeEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
        canDrawOverlays = Settings.canDrawOverlays(context)
        isClippyRunning = ClippyOverlayService.isRunning
        onPauseOrDispose {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "slop detector setup",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusRow(label = "Screen reading (accessibility)", ok = isAccessibilityEnabled)
        StatusRow(label = "Draw over apps (Clippy)", ok = canDrawOverlays)
        StatusRow(label = "Clippy running", ok = isClippyRunning)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        ) {
            Text(text = "1. Enable screen reading")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        ) {
            Text(text = "2. Allow Clippy to draw over apps")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canDrawOverlays && isAccessibilityEnabled,
            onClick = {
                if (isClippyRunning) {
                    ClippyOverlayService.stop(context)
                    isClippyRunning = false
                } else {
                    ClippyOverlayService.start(context)
                    isClippyRunning = true
                }
            },
        ) {
            Text(text = if (isClippyRunning) "3. Stop Clippy" else "3. Start Clippy")
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, SlopboardAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (ok) "✓" else "✗",
            color = if (ok) Color(0xFF2E7D32) else Color(0xFFC62828),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = "  $label",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
