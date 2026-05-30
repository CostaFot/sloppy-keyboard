package com.markedusduplicate.slopboard.keyboard.suggestion

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import com.markedusduplicate.slopboard.suggestion.Suggestions
import com.markedusduplicate.slopboard.ui.activity.MainActivity
import dagger.hilt.android.EntryPointAccessors

private val STRIP_HEIGHT = 48.dp

/**
 * The strip above the key grid. When there are predictions it shows up to three tappable chips
 * (n-gram = secondary container, LLM = primary container so model predictions stand out); when
 * idle it shows a Gboard-style tools row. Most tools are placeholders for now — the settings cog
 * opens the keyboard's setup screen.
 */
@Composable
fun SuggestionBar(modifier: Modifier = Modifier) {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, SuggestionViewModelEntryPoint::class.java)
            .suggestionViewModel()
    }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    SuggestionStrip(suggestions = suggestions, onAccept = viewModel::onAccept, modifier = modifier)
}

@Composable
private fun SuggestionStrip(
    suggestions: Suggestions,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(STRIP_HEIGHT)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (suggestions.words.isEmpty()) {
            IdleToolbar(Modifier.fillMaxSize())
        } else {
            SuggestionChips(suggestions, onAccept, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SuggestionChips(
    suggestions: Suggestions,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val color = if (suggestions.fromLlm) scheme.primaryContainer else scheme.secondaryContainer
    val contentColor =
        if (suggestions.fromLlm) scheme.onPrimaryContainer else scheme.onSecondaryContainer

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        suggestions.words.forEach { word ->
            SuggestionChip(
                word = word,
                color = color,
                contentColor = contentColor,
                onClick = { onAccept(word) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun IdleToolbar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Icons.Filled.Apps, "Apps") {}
        ToolButton(Icons.Filled.EmojiEmotions, "Emoji") {}
        ToolButton(Icons.Filled.Gif, "GIF") {}
        ToolButton(Icons.Filled.AutoFixHigh, "Suggestions") {}
        ToolButton(Icons.Filled.ContentPaste, "Clipboard") {}
        ToolButton(Icons.Filled.Settings, "Settings") {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        ToolButton(Icons.Filled.Mic, "Voice") {}
    }
}

@Composable
private fun ToolButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun SuggestionChip(
    word: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = word,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun IdleToolbarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(suggestions = Suggestions.EMPTY, onAccept = {})
        }
    }
}

@Preview
@Composable
private fun NgramSuggestionBarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = false),
                onAccept = {},
            )
        }
    }
}

@Preview
@Composable
private fun LlmSuggestionBarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = true),
                onAccept = {},
            )
        }
    }
}
