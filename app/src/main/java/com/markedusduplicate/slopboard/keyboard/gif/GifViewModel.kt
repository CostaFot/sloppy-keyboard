package com.markedusduplicate.slopboard.keyboard.gif

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.accessibility.ScreenContextHolder
import com.markedusduplicate.slopboard.domain.GifRepository
import com.markedusduplicate.slopboard.domain.model.GifItem
import com.markedusduplicate.slopboard.keyboard.KeyboardHandler
import com.markedusduplicate.slopboard.keyboard.KeyboardMessage
import com.markedusduplicate.slopboard.retain.RetainedViewModel
import com.markedusduplicate.slopboard.suggestion.llm.GifPrompt
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What the gif screen shows: choosing queries, the query chips + gif tray, an error, or no context. */
sealed interface GifUiState {
    data object Loading : GifUiState
    data object NoContext : GifUiState
    data class Ready(
        val queries: List<String>,
        val selected: String,
        val gifs: List<GifItem>,
        val gifsLoading: Boolean,
    ) : GifUiState

    data class Failed(val message: String) : GifUiState
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GifViewModelEntryPoint {
    fun gifViewModel(): GifViewModel
}

/**
 * Turns the captured screen text into Giphy search queries via the on-device LLM, fetches gifs for
 * the selected query, and inserts the chosen gif into the field (rich content). LLM + network run on
 * the IO dispatcher.
 */
class GifViewModel @Inject constructor(
    private val engine: LlmEngine,
    private val screenContextHolder: ScreenContextHolder,
    private val repository: GifRepository,
    private val keyboardHandler: KeyboardHandler,
    private val dispatcherProvider: DispatcherProvider,
) : RetainedViewModel() {

    private val _state = MutableStateFlow<GifUiState>(GifUiState.Loading)
    val state: StateFlow<GifUiState> = _state.asStateFlow()

    fun generate() {
        val context = screenContextHolder.screenText.value.trim()
        if (context.isEmpty()) {
            _state.value = GifUiState.NoContext
            return
        }
        _state.value = GifUiState.Loading
        viewModelScope.launch {
            val queries = withContext(dispatcherProvider.io) {
                val activeEngine = engine.engineOrNull() ?: return@withContext null
                runCatching {
                    activeEngine.createConversation().use { conversation ->
                        GifPrompt.parse(conversation.sendMessage(GifPrompt.queries(context)).toString())
                    }
                }.getOrNull()
            }
            when {
                queries == null -> _state.value = GifUiState.Failed("Model still loading…")
                queries.isEmpty() -> _state.value = GifUiState.Failed("No search ideas produced")
                else -> selectQuery(queries.first(), queries)
            }
        }
    }

    fun selectQuery(query: String, queries: List<String> = currentQueries()) {
        _state.value = GifUiState.Ready(queries, query, gifs = emptyList(), gifsLoading = true)
        viewModelScope.launch {
            _state.value = try {
                GifUiState.Ready(queries, query, repository.search(query), gifsLoading = false)
            } catch (t: Throwable) {
                logDebug { "Giphy search failed: ${t.message}" }
                GifUiState.Failed(t.message ?: "Gif search failed")
            }
        }
    }

    fun pick(gif: GifItem) {
        viewModelScope.launch {
            runCatching {
                val uri = repository.downloadToCache(gif.gifUrl)
                keyboardHandler.queue.emit(KeyboardMessage.CommitGif(uri, MIME_GIF, gif.description))
            }.onFailure { logDebug { "Gif insert failed: ${it.message}" } }
        }
    }

    private fun currentQueries(): List<String> =
        (_state.value as? GifUiState.Ready)?.queries ?: emptyList()

    private companion object {
        const val MIME_GIF = "image/gif"
    }
}
