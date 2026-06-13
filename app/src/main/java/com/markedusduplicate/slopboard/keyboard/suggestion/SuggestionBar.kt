package com.markedusduplicate.slopboard.keyboard.suggestion

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import com.markedusduplicate.slopboard.suggestion.SuggestionMode
import com.markedusduplicate.slopboard.suggestion.SuggestionState
import com.markedusduplicate.slopboard.suggestion.Suggestions
import com.markedusduplicate.slopboard.ui.activity.MainActivity
import dagger.hilt.android.EntryPointAccessors

private val ROW_HEIGHT = 44.dp
private const val SLOTS = 3
private const val TBD_SUGGESTION = "TBD suggestion"
private const val TBD_LLM = "TBD llm"

/**
 * The single suggestion row above the key grid. While typing a word it shows the dictionary's
 * completions / spelling fix (the fix is bold); after a space it shows the user's next-word n-grams
 * in the first two slots and the LLM's next word in the third — empty slots fall back to debug
 * placeholders ("TBD suggestion" / "TBD llm"). It uses [SLOTS] fixed equal-width cells so nothing
 * reflows. When the field is empty it shows a Gboard-style tools row (the cog opens setup).
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!state.active) {
            IdleToolbar(Modifier.fillMaxSize())
        } else {
            SuggestionRow(slots = slotsFor(state), onAccept = onAccept, modifier = Modifier.fillMaxSize())
        }
    }
}

/** What a single cell shows: a tappable word, a non-tappable debug placeholder, or nothing. */
private sealed interface Slot {
    data class Word(val text: String, val highlighted: Boolean, val fromLlm: Boolean) : Slot
    data class Placeholder(val text: String) : Slot
    data object Empty : Slot
}

private fun slotsFor(state: SuggestionState): List<Slot> = when (state.mode) {
    SuggestionMode.TYPING -> (0 until SLOTS).map { i ->
        val word = state.dictionary.words.getOrNull(i)
        if (word == null) {
            Slot.Empty
        } else {
            Slot.Word(word, highlighted = word == state.dictionary.correction, fromLlm = false)
        }
    }

    SuggestionMode.NEXT_WORD -> listOf(
        dbSlot(state.dictionary.words.getOrNull(0)),
        dbSlot(state.dictionary.words.getOrNull(1)),
        state.llmNextWord?.let { Slot.Word(it, highlighted = false, fromLlm = true) }
            ?: Slot.Placeholder(TBD_LLM),
    )
}

private fun dbSlot(word: String?): Slot =
    word?.let { Slot.Word(it, highlighted = false, fromLlm = false) } ?: Slot.Placeholder(TBD_SUGGESTION)

@Composable
private fun SuggestionRow(
    slots: List<Slot>,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEach { slot ->
            SuggestionSlot(slot = slot, onAccept = onAccept, modifier = Modifier
                .weight(1f)
                .fillMaxHeight())
        }
    }
}

@Composable
private fun SuggestionSlot(
    slot: Slot,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    when (slot) {
        is Slot.Empty -> Box(modifier)

        is Slot.Placeholder -> ChipSurface(
            modifier = modifier,
            color = scheme.surfaceVariant,
            contentColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            onClick = null,
        ) {
            SlotText(slot.text, italic = true)
        }

        is Slot.Word -> ChipSurface(
            modifier = modifier,
            color = if (slot.fromLlm) scheme.primaryContainer else scheme.secondaryContainer,
            contentColor = if (slot.fromLlm) scheme.onPrimaryContainer else scheme.onSecondaryContainer,
            onClick = { onAccept(slot.text) },
        ) {
            SlotText(slot.text, bold = slot.highlighted)
        }
    }
}

@Composable
private fun ChipSurface(
    color: Color,
    contentColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val inner: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color, contentColor = contentColor) { inner() }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, contentColor = contentColor) {
            inner()
        }
    }
}

@Composable
private fun SlotText(text: String, bold: Boolean = false, italic: Boolean = false) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
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
private fun TypingRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                state = SuggestionState(
                    active = true,
                    mode = SuggestionMode.TYPING,
                    dictionary = Suggestions(listOf("the", "then", "they"), correction = "the"),
                    llmNextWord = null,
                ),
                onAccept = {},
            )
        }
    }
}

@Preview
@Composable
private fun NextWordRowPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SuggestionStrip(
                state = SuggestionState(
                    active = true,
                    mode = SuggestionMode.NEXT_WORD,
                    dictionary = Suggestions(listOf("morning")),
                    llmNextWord = null,
                ),
                onAccept = {},
            )
        }
    }
}
