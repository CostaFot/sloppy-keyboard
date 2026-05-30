package com.markedusduplicate.slopboard.keyboard.observe

import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.slopboard.keyboard.observe.TextContext.CONTEXT_WORDS
import com.markedusduplicate.slopboard.suggestion.PersonalizationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Silently learns from typing by diffing successive "text before cursor" snapshots.
 *
 * Each time a word becomes finalized (a boundary appears after it) we record the (context ->
 * word) n-gram. If finalized words disappear (the user backspaced one) and a *different* word is
 * then finalized in the same context, we record that as a correction. Best-effort: it is driven
 * by snapshots, so it tolerates—but doesn't perfectly reconstruct—arbitrary cursor edits.
 *
 * All mutable bookkeeping is touched only from the service's single input thread; DB writes are
 * dispatched to IO.
 */
@Singleton
class ObservationManager @Inject constructor(
    private val repository: PersonalizationRepository,
    private val tracker: InputContextTracker,
    @ApplicationCoroutineScope private val scope: CoroutineScope,
) {
    private var lastFinalized: List<String> = emptyList()
    private var pendingCorrection: PendingCorrection? = null

    fun reset() {
        lastFinalized = emptyList()
        pendingCorrection = null
    }

    fun onTextBeforeCursor(text: String) {
        val finalized = TextContext.finalizedWords(text)
        if (!tracker.allowed.value) {
            lastFinalized = finalized
            pendingCorrection = null
            return
        }

        when {
            finalized.size > lastFinalized.size && sharesPrefix(finalized, lastFinalized) -> {
                for (i in lastFinalized.size until finalized.size) {
                    val context = contextKeyAt(finalized, i)
                    val word = finalized[i]
                    record { repository.recordNgram(context, word) }

                    pendingCorrection?.let { pc ->
                        if (pc.context == context && !pc.deletedWord.equals(word, ignoreCase = true)) {
                            record { repository.recordCorrection(pc.deletedWord, word) }
                        }
                    }
                    pendingCorrection = null
                }
            }

            finalized.size < lastFinalized.size && sharesPrefix(lastFinalized, finalized) -> {
                val idx = finalized.size
                pendingCorrection = PendingCorrection(
                    context = contextKeyAt(lastFinalized, idx),
                    deletedWord = lastFinalized[idx],
                )
            }
        }
        lastFinalized = finalized
    }

    private fun contextKeyAt(words: List<String>, index: Int): String =
        TextContext.contextKey(words.subList(maxOf(0, index - CONTEXT_WORDS), index))

    private fun sharesPrefix(longer: List<String>, shorter: List<String>): Boolean =
        shorter.indices.all { longer[it].equals(shorter[it], ignoreCase = true) }

    private inline fun record(crossinline block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private data class PendingCorrection(val context: String, val deletedWord: String)
}
