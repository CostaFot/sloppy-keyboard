package com.markedusduplicate.slopboard.keyboard.main

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.keyboard.suggestion.SuggestionBar
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private val LetterRows = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m"),
)

private val KeyShape = RoundedCornerShape(10.dp)
private const val ROW_HEIGHT = 50
private const val KEY_GAP = 5

// Press-and-hold auto-repeat for the backspace key (Gboard-style): delay before repeating, then
// fire every interval, accelerating down to a floor the longer it's held.
private const val DELETE_INITIAL_DELAY_MS = 280L
private const val DELETE_REPEAT_INTERVAL_MS = 60L
private const val DELETE_MIN_INTERVAL_MS = 20L
private const val DELETE_ACCEL_STEP_MS = 3L

@Composable
fun KeyboardScreen() {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, KeyboardViewModelEntryPoint::class.java)
            .keyboardViewModel()
    }

    KeyboardLayout(onText = viewModel::onText, onDelete = viewModel::onDelete)

    DisposableEffect(Unit) {
        onDispose { Timber.tag("KeyboardComposeView").d("keyboard left composition") }
    }
}

@Composable
private fun KeyboardLayout(
    onText: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SuggestionBar()
            KeyGrid(onText = onText, onDelete = onDelete)
        }
    }
}

@Composable
private fun KeyGrid(
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shifted by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    fun commitLetter(label: String) {
        onText(label)
        if (shifted) shifted = false
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KEY_GAP.dp)) {
        KeyRow {
            LetterRows[0].forEach { LetterKey(it, shifted, Modifier.weight(1f), ::commitLetter) }
        }
        KeyRow {
            Spacer(Modifier.weight(0.5f))
            LetterRows[1].forEach { LetterKey(it, shifted, Modifier.weight(1f), ::commitLetter) }
            Spacer(Modifier.weight(0.5f))
        }
        KeyRow {
            IconKey(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Shift",
                modifier = Modifier.weight(1.5f),
                color = if (shifted) scheme.primary else scheme.secondaryContainer,
                contentColor = if (shifted) scheme.onPrimary else scheme.onSecondaryContainer,
                onClick = { shifted = !shifted },
            )
            LetterRows[2].forEach { LetterKey(it, shifted, Modifier.weight(1f), ::commitLetter) }
            RepeatingIconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.weight(1.5f),
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                onClick = onDelete,
            )
        }
        KeyRow {
            TextKey(",", Modifier.weight(1f), scheme.secondaryContainer, scheme.onSecondaryContainer) { onText(",") }
            TextKey("space", Modifier.weight(5f), scheme.surfaceContainerHighest, scheme.onSurface) { onText(" ") }
            TextKey(".", Modifier.weight(1f), scheme.secondaryContainer, scheme.onSecondaryContainer) { onText(".") }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT.dp),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP.dp),
        content = content,
    )
}

@Composable
private fun LetterKey(
    letter: String,
    shifted: Boolean,
    modifier: Modifier,
    onCommit: (String) -> Unit,
) {
    val label = if (shifted) letter.uppercase() else letter
    val scheme = MaterialTheme.colorScheme
    KeyButton(modifier, scheme.surfaceContainerHighest, scheme.onSurface, onClick = { onCommit(label) }) {
        Text(text = label, fontSize = 18.sp)
    }
}

@Composable
private fun TextKey(
    label: String,
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    KeyButton(modifier, color, contentColor, onClick) {
        Text(text = label, fontSize = 15.sp)
    }
}

@Composable
private fun IconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    KeyButton(modifier, color, contentColor, onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

/**
 * An icon key that fires [onClick] once on tap and, while held, auto-repeats it — after
 * [DELETE_INITIAL_DELAY_MS], then every interval (accelerating toward [DELETE_MIN_INTERVAL_MS]).
 * Drives the ripple from the same press so it still looks like a normal key.
 */
@Composable
private fun RepeatingIconKey(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        try {
                            currentOnClick()
                            if (withTimeoutOrNull(DELETE_INITIAL_DELAY_MS) { tryAwaitRelease() } == null) {
                                var interval = DELETE_REPEAT_INTERVAL_MS
                                while (true) {
                                    currentOnClick()
                                    if (withTimeoutOrNull(interval) { tryAwaitRelease() } != null) break
                                    interval = (interval - DELETE_ACCEL_STEP_MS)
                                        .coerceAtLeast(DELETE_MIN_INTERVAL_MS)
                                }
                            }
                        } finally {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    },
                )
            },
        shape = KeyShape,
        color = color,
        contentColor = contentColor,
        shadowElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun KeyButton(
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = KeyShape,
        color = color,
        contentColor = contentColor,
        shadowElevation = 1.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Preview
@Composable
private fun KeyGridPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            KeyGrid(onText = {}, onDelete = {}, modifier = Modifier.padding(8.dp))
        }
    }
}
