package com.markedusduplicate.slopboard.ui.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.material3.TextField
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
    val inputMethodManager = remember(context) {
        context.getSystemService(InputMethodManager::class.java)
    }

    var isEnabled by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }

    // Re-read status every time the Activity resumes, so returning from system
    // settings / the IME picker refreshes the indicators.
    LifecycleResumeEffect(Unit) {
        isEnabled = inputMethodManager.enabledInputMethodList
            .any { it.packageName == context.packageName }
        isSelected = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        )?.startsWith(context.packageName) == true
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
            text = "slopboard setup",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusRow(label = "Enabled", ok = isEnabled)
        StatusRow(label = "Selected as default", ok = isSelected)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
        ) {
            Text(text = "1. Enable slopboard")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                inputMethodManager.showInputMethodPicker()
            },
        ) {
            Text(text = "2. Select slopboard")
        }

        Spacer(modifier = Modifier.height(24.dp))

        var text by remember { mutableStateOf("") }
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Test the keyboard") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
