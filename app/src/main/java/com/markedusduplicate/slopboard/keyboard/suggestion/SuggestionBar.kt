package com.markedusduplicate.slopboard.keyboard.suggestion

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import com.markedusduplicate.slopboard.suggestion.SuggestionState
import com.markedusduplicate.slopboard.suggestion.Suggestions
import com.markedusduplicate.slopboard.ui.activity.MainActivity
import dagger.hilt.android.EntryPointAccessors

private val ROW_HEIGHT = 44.dp
private const val SLOTS = 3

/**
 * The area above the key grid. While typing it shows two stable, fixed-slot rows so nothing jumps:
 * the instant **dictionary** row (secondary container) on top, and — when it has results — the
 * **LLM** row (primary container) below. Each row is always [SLOTS] equal-width cells (blank cells
 * stay blank, so the layout never reflows); the row's best word sits in the centre, bold. When the
 * field is empty it shows a Gboard-style tools row (the settings cog opens the keyboard's setup
 * screen).
 */
@Composable
fun SuggestionBar(modifier: Modifier = Modifier) {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, SuggestionViewModelEntryPoint::class.java)
            .suggestionViewModel()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SuggestionStrip(state = state, onAccept = viewModel::onAccept, modifier = modifier)
}

@Composable
private fun SuggestionStrip(
    state: SuggestionState,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!state.active) {
            IdleToolbar(Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT))
        } else {
            SuggestionRow(
                suggestions = state.dictionary,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                onAccept = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT),
            )
            if (state.llm.words.isNotEmpty()) {
                SuggestionRow(
                    suggestions = state.llm,
                    color = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                    onAccept = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT),
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestions: Suggestions,
    color: Color,
    contentColor: Color,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = arrangeSlots(suggestions)
    val best = suggestions.correction ?: suggestions.words.firstOrNull()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEach { word ->
            SuggestionSlot(
                word = word,
                highlighted = word != null && word == best,
                color = color,
                contentColor = contentColor,
                onAccept = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/** Up to three words laid out into fixed slots with the [Suggestions.correction]/best word centred. */
private fun arrangeSlots(suggestions: Suggestions): List<String?> {
    val best = suggestions.correction ?: suggestions.words.firstOrNull()
    val others = suggestions.words.filter { it != best }
    return listOf(others.getOrNull(0), best, others.getOrNull(1))
}

@Composable
private fun SuggestionSlot(
    word: String?,
    highlighted: Boolean,
    color: Color,
    contentColor: Color,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (word == null) {
        Box(modifier) // blank cell keeps the row at three equal columns
        return
    }
    Surface(
        onClick = { onAccept(word) },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = word,
                fontSize = 15.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
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

@Preview
@Composable
private fun IdleToolbarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(state = SuggestionState.EMPTY, onAccept = {})
        }
    }
}

@Preview
@Composable
private fun DictionaryRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                state = SuggestionState(
                    active = true,
                    dictionary = Suggestions(listOf("Android", "androids", "andromeda")),
                    llm = Suggestions.EMPTY,
                ),
                onAccept = {},
            )
        }
    }
}

@Preview
@Composable
private fun TwoRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                state = SuggestionState(
                    active = true,
                    dictionary = Suggestions(listOf("the", "then", "they"), correction = "the"),
                    llm = Suggestions(listOf("there", "their", "they")),
                ),
                onAccept = {},
            )
        }
    }
}
