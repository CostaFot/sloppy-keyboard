package com.markedusduplicate.slopboard.keyboard.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import com.markedusduplicate.slopboard.suggestion.Suggestions
import dagger.hilt.android.EntryPointAccessors

private val NgramChipColor = Color(0xFF6C6565)
private val LlmChipColor = Color(0xFF5C6BC0)

/**
 * The three predictive-text chips rendered above the key grid. Collapses to nothing when there
 * is no suggestion for the current context.
 */
@Composable
fun SuggestionBar(modifier: Modifier = Modifier) {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, SuggestionViewModelEntryPoint::class.java)
            .suggestionViewModel()
    }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    SuggestionBarContent(
        suggestions = suggestions,
        onAccept = viewModel::onAccept,
        modifier = modifier,
    )
}

@Composable
private fun SuggestionBarContent(
    suggestions: Suggestions,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.words.isEmpty()) return

    val chipColor = if (suggestions.fromLlm) LlmChipColor else NgramChipColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        suggestions.words.forEach { word ->
            SuggestionChip(
                word = word,
                color = chipColor,
                onClick = { onAccept(word) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    word: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = color,
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
                color = Color.White,
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
private fun NgramSuggestionBarPreview() {
    SuggestionBarContent(
        suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = false),
        onAccept = {},
    )
}

@Preview
@Composable
private fun LlmSuggestionBarPreview() {
    SuggestionBarContent(
        suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = true),
        onAccept = {},
    )
}
