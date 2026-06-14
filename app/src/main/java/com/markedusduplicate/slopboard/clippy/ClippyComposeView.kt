package com.markedusduplicate.slopboard.clippy

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.R
import kotlinx.coroutines.flow.StateFlow

/**
 * The floating mascot itself: a draggable 📎 emoji with a speech bubble that shows whatever the LLM
 * just said. Rendered into a `WindowManager` overlay by [ClippyOverlayService], so the content is
 * wrap-sized and the background stays transparent — only the emoji and bubble occupy (and intercept
 * touches in) the window; everything else passes through to the app beneath.
 */
@SuppressLint("ViewConstructor")
class ClippyComposeView(
    context: Context,
    private val state: StateFlow<ClippyState>,
    private val onTap: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDismiss: () -> Unit,
) : AbstractComposeView(context) {

    init {
        id = R.id.clippyComposeView
    }

    @Composable
    override fun Content() {
        val s by state.collectAsStateWithLifecycle()
        AppTheme {
            if (s == ClippyState.Hidden) return@AppTheme
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "📎",
                    fontSize = 40.sp,
                    modifier = Modifier
                        .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, amount ->
                                change.consume()
                                onDrag(amount.x, amount.y)
                            }
                        },
                )

                val bubble = when (val current = s) {
                    ClippyState.Hidden -> null
                    ClippyState.Thinking -> "🤔 …"
                    is ClippyState.Speaking -> current.remark
                    is ClippyState.Unavailable -> current.reason
                }
                if (bubble != null) Bubble(text = bubble, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun Bubble(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp,
        modifier = Modifier.widthIn(max = 240.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
