package com.markedusduplicate.slopboard.suggestion

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.suggestion.llm.LlmEngine
import com.markedusduplicate.slopboard.suggestion.llm.SuggestionPrompt
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Next-word prediction from the local LiteRT-LM model. Full-freedom: the model is given only the
 * text so far and asked for the single next word — no personalization hints. Returns
 * [Suggestions.EMPTY] when no model is loaded or inference fails, so the slot falls back to its
 * placeholder rather than showing stale chips. (The coordinator only invokes this on a boundary.)
 */
class LlmSuggestionSource @Inject constructor(
    private val engine: LlmEngine,
    private val dispatcherProvider: DispatcherProvider,
) : SuggestionSource {

    override suspend fun suggest(textBeforeCursor: String): Suggestions =
        withContext(dispatcherProvider.io) {
            if (textBeforeCursor.isBlank()) return@withContext Suggestions.EMPTY
            val activeEngine = engine.engineOrNull() ?: return@withContext Suggestions.EMPTY

            val prompt = SuggestionPrompt.nextWord(textBeforeCursor)
            logDebug { "LLM prompt:\n$prompt" }
            try {
                activeEngine.createConversation().use { conversation ->
                    val reply = conversation.sendMessage(prompt).toString()
                    logDebug { "LLM reply: $reply" }
                    SuggestionPrompt.parse(reply)
                }
            } catch (t: Throwable) {
                logDebug { "LLM inference failed: ${t.message}" }
                Suggestions.EMPTY
            }
        }
}
