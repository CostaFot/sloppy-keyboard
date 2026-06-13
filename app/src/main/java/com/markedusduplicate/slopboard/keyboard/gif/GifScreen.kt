package com.markedusduplicate.slopboard.keyboard.gif

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.markedusduplicate.slopboard.domain.model.GifItem
import com.markedusduplicate.slopboard.retain.rememberRetainedViewModel
import dagger.hilt.android.EntryPointAccessors

// Fixed, bounded gif tray (Gboard-style) so large gifs can't balloon the keyboard vertically.
private val GIF_AREA_HEIGHT = 200.dp
private val GIF_TILE = 100.dp

/**
 * Reads the captured screen text, asks the LLM for gif search queries, and shows them as chips over a
 * tray of gifs. Tapping a gif inserts it into the field (rich content) and returns to the keyboard.
 */
@Composable
fun GifScreen(onDone: () -> Unit) {
    val viewModel = rememberRetainedViewModel { context ->
        EntryPointAccessors.fromApplication(context, GifViewModelEntryPoint::class.java)
            .gifViewModel()
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
                GifUiState.Loading -> StatusContent("Finding gifs…")

                GifUiState.NoContext -> RetryContent(
                    message = "No screen text captured yet. Enable the accessibility service, then " +
                            "open a chat and tap into its message field.",
                    onRetry = viewModel::generate,
                )

                is GifUiState.Failed -> RetryContent(current.message, viewModel::generate)

                is GifUiState.Ready -> GifReady(
                    state = current,
                    onSelectQuery = { viewModel.selectQuery(it) },
                    onPick = { gif ->
                        viewModel.pick(gif)
                        onDone()
                    },
                )
            }
        }
    }
}

@Composable
private fun GifReady(
    state: GifUiState.Ready,
    onSelectQuery: (String) -> Unit,
    onPick: (GifItem) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.queries.forEach { query ->
                FilterChip(
                    selected = query == state.selected,
                    onClick = { onSelectQuery(query) },
                    label = { Text(query) },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GIF_AREA_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.gifsLoading -> CircularProgressIndicator()

                state.gifs.isEmpty() -> Text(
                    text = "No gifs found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = GIF_TILE),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.gifs) { gif ->
                        AsyncImage(
                            model = gif.previewUrl,
                            contentDescription = gif.description,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPick(gif) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusContent(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
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
