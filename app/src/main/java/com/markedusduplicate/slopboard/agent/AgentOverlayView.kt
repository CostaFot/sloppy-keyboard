package com.markedusduplicate.slopboard.agent

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.R

/**
 * Full-screen, mostly pass-through overlay that renders the suggestion loop: a highlight box around
 * the element Clippy wants to act on, plus a chip that names the action and lets the user run it. The
 * window is only made touchable (by [com.markedusduplicate.slopboard.clippy.ClippyOverlayService])
 * while a suggestion or result is showing, so it doesn't block the phone the rest of the time.
 */
@SuppressLint("ViewConstructor")
class AgentOverlayView(
    context: Context,
    private val engine: AgentEngine,
) : AbstractComposeView(context) {

    // This view's top-left in real screen coordinates. Accessibility bounds are screen-absolute (y=0
    // above the status bar) while the overlay's content can start below it, so we subtract this origin
    // when drawing — robust to status bars, cutouts, and gesture insets.
    private val originState = mutableStateOf(IntOffset.Zero)

    init {
        id = R.id.agentOverlayView
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateOrigin() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateOrigin()
    }

    private fun updateOrigin() {
        val location = IntArray(2)
        getLocationOnScreen(location)
        originState.value = IntOffset(location[0], location[1])
    }

    @Composable
    override fun Content() {
        val state by engine.state.collectAsStateWithLifecycle()
        AppTheme {
            when (val s = state) {
                AgentState.Idle, AgentState.Acting -> Unit
                AgentState.Thinking -> BottomChip("🤔 reading the screen…")
                is AgentState.Suggest -> Spotlight(s, originState.value, engine::accept, engine::close)
                is AgentState.Done -> BottomChip(s.message, onClose = engine::close)
                is AgentState.Failed -> BottomChip(s.message, onClose = engine::close)
            }
        }
    }
}

@Composable
private fun Spotlight(
    state: AgentState.Suggest,
    origin: IntOffset,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val accent = MaterialTheme.colorScheme.primary
    val target = state.target
    var chipSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = with(density) { maxWidth.roundToPx() }
        val maxH = with(density) { maxHeight.roundToPx() }
        val gap = with(density) { 8.dp.roundToPx() }
        val margin = with(density) { 24.dp.roundToPx() }

        if (target != null) {
            // Translate screen-absolute bounds into this view's local space.
            val left = target.left - origin.x
            val top = target.top - origin.y
            val width = target.right - target.left
            val height = target.bottom - target.top
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topLeft = Offset(left.toFloat(), top.toFloat())
                val boxSize = Size(width.toFloat(), height.toFloat())
                val radius = CornerRadius(24f, 24f)
                drawRoundRect(color = accent.copy(alpha = 0.18f), topLeft = topLeft, size = boxSize, cornerRadius = radius)
                drawRoundRect(color = accent, topLeft = topLeft, size = boxSize, cornerRadius = radius, style = Stroke(width = 6f))
            }
            // Tapping the spotlighted element itself runs the action.
            Box(
                modifier = Modifier
                    .offset { IntOffset(left, top) }
                    .size(
                        width = with(density) { width.toDp() },
                        height = with(density) { height.toDp() },
                    )
                    .clickable(onClick = onAccept),
            )
        }

        // Place the chip below the target, flipping above it when there's no room; clamp to the
        // screen so it can never render off the bottom or the right edge.
        val chipOffset = if (target != null) {
            val below = target.bottom - origin.y + gap
            val above = target.top - origin.y - chipSize.height - gap
            val y = if (below + chipSize.height <= maxH) below else above.coerceAtLeast(0)
            val x = (target.left - origin.x).coerceIn(0, (maxW - chipSize.width).coerceAtLeast(0))
            IntOffset(x, y)
        } else {
            IntOffset(
                x = ((maxW - chipSize.width) / 2).coerceAtLeast(0),
                y = (maxH - chipSize.height - margin).coerceAtLeast(0),
            )
        }
        SuggestionChip(
            label = state.label,
            onAccept = onAccept,
            onDismiss = onDismiss,
            modifier = Modifier
                .offset { chipOffset }
                .onSizeChanged { chipSize = it },
        )
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 300.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "👉 $label", modifier = Modifier.weight(1f, fill = false))
            Button(onClick = onAccept) { Text("Do it") }
            TextButton(onClick = onDismiss) { Text("✕") }
        }
    }
}

@Composable
private fun BottomChip(text: String, onClose: (() -> Unit)? = null) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .widthIn(max = 320.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = text, modifier = Modifier.weight(1f, fill = false))
                if (onClose != null) TextButton(onClick = onClose) { Text("OK") }
            }
        }
    }
}
