package com.markedusduplicate.slopboard.keyboard.suggestion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import com.markedusduplicate.slopboard.suggestion.Suggestions
import dagger.hilt.android.EntryPointAccessors

/**
 * The three predictive-text chips rendered above the key grid. Collapses to nothing when there is
 * no suggestion for the current context. n-gram suggestions use the secondary container; LLM
 * suggestions use the primary container so they stand out as model predictions.
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

    val scheme = MaterialTheme.colorScheme
    val color = if (suggestions.fromLlm) scheme.primaryContainer else scheme.secondaryContainer
    val contentColor =
        if (suggestions.fromLlm) scheme.onPrimaryContainer else scheme.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp),
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
private fun NgramSuggestionBarPreview() {
    AppTheme {
        SuggestionBarContent(
            suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = false),
            onAccept = {},
        )
    }
}

@Preview
@Composable
private fun LlmSuggestionBarPreview() {
    AppTheme {
        SuggestionBarContent(
            suggestions = Suggestions(listOf("hell", "heck", "help"), fromLlm = true),
            onAccept = {},
        )
    }
}
