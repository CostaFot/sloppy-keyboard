package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.keyboard.observe.TextContext
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.SuggestionPrompt
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Next-word suggestions from the local LiteRT-LM model, personalized by injecting the user's
 * n-gram hints into the prompt (RAG). Returns an empty list when no model is loaded or inference
 * fails, so the keyboard silently falls back to the n-gram source.
 */
class LlmSuggestionSource @Inject constructor(
    private val engine: LlmEngine,
    private val personalization: PersonalizationRepository,
    private val dispatcherProvider: DispatcherProvider,
) : SuggestionSource {

    override suspend fun suggest(textBeforeCursor: String): List<String> =
        withContext(dispatcherProvider.io) {
            val context = TextContext.predictionContext(textBeforeCursor)
            if (context.isBlank()) return@withContext emptyList()
            val activeEngine = engine.engineOrNull() ?: return@withContext emptyList()

            val hints = personalization.suggest(context, TextContext.currentPrefix(textBeforeCursor))
            val prompt = SuggestionPrompt.build(textBeforeCursor, context, hints)
            try {
                activeEngine.createConversation().use { conversation ->
                    SuggestionPrompt.parse(conversation.sendMessage(prompt).toString())
                }
            } catch (t: Throwable) {
                logDebug { "LLM inference failed: ${t.message}" }
                emptyList()
            }
        }
}
