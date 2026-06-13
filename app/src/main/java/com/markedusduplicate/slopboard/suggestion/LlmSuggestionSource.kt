package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.keyboard.observe.TextContext
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.SuggestionPrompt
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The keyboard's only chip source: completions and spelling fixes for the word being typed, or
 * next-word predictions on a boundary, from the local LiteRT-LM model. Personalized by injecting
 * the user's n-gram hints into the prompt (RAG). Returns [Suggestions.EMPTY] when no model is
 * loaded or inference fails, so the strip stays empty rather than showing stale chips.
 */
class LlmSuggestionSource @Inject constructor(
    private val engine: LlmEngine,
    private val personalization: PersonalizationRepository,
    private val dispatcherProvider: DispatcherProvider,
) : SuggestionSource {

    override suspend fun suggest(textBeforeCursor: String): Suggestions =
        withContext(dispatcherProvider.io) {
            val context = TextContext.predictionContext(textBeforeCursor)
            val prefix = TextContext.currentPrefix(textBeforeCursor)
            if (context.isBlank() && prefix.isBlank()) return@withContext Suggestions.EMPTY
            val activeEngine = engine.engineOrNull() ?: return@withContext Suggestions.EMPTY

            val hints = personalization.suggest(context, prefix)
            val prompt = SuggestionPrompt.build(textBeforeCursor, context, prefix, hints)
            logDebug { "LLM prompt:\n$prompt" }
            try {
                activeEngine.createConversation().use { conversation ->
                    val reply = conversation.sendMessage(prompt).toString()
                    logDebug { "LLM reply: $reply" }
                    SuggestionPrompt.parse(reply, prefix)
                }
            } catch (t: Throwable) {
                logDebug { "LLM inference failed: ${t.message}" }
                Suggestions.EMPTY
            }
        }
}
