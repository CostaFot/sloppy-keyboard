package com.markedusduplicate.slopboard.keyboard.reply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import dagger.hilt.android.EntryPointAccessors

/**
 * Reads the captured screen text, asks the LLM for one complete reply, and shows it in a big card.
 * Tapping the card inserts the reply into the field and returns to the keyboard ([onDone]).
 */
@Composable
fun SmartReplyScreen(onDone: () -> Unit) {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, SmartReplyViewModelEntryPoint::class.java)
            .smartReplyViewModel()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.generate() }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                SmartReplyUiState.Loading -> StatusContent("Drafting a reply…", showSpinner = true)

                SmartReplyUiState.NoContext -> RetryContent(
                    message = "No screen text captured yet. Enable the accessibility service, then " +
                            "open a chat and tap into its message field.",
                    onRetry = viewModel::generate,
                )

                is SmartReplyUiState.Failed -> RetryContent(
                    message = current.message,
                    onRetry = viewModel::generate,
                )

                is SmartReplyUiState.Ready -> ReplyCard(
                    reply = current.reply,
                    onAccept = {
                        viewModel.commit(current.reply)
                        onDone()
                    },
                    onRegenerate = viewModel::generate,
                )
            }
        }
    }
}

@Composable
private fun ReplyCard(reply: String, onAccept: () -> Unit, onRegenerate: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 170.dp),
            shape = RoundedCornerShape(16.dp),
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(text = reply, fontSize = 16.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tap the card to insert",
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
            TextButton(onClick = onRegenerate) { Text("Regenerate") }
        }
    }
}

@Composable
private fun StatusContent(text: String, showSpinner: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSpinner) CircularProgressIndicator()
        Text(text = text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RetryContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry) { Text("Retry") }
    }
}
