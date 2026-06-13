package com.markedusduplicate.slopboard.suggestion.dictionary

import android.content.Context
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the bundled-dictionary [WordIndex] for the app session. The asset is parsed once on a
 * background thread (warm-up runs on the application scope, like [com.markedusduplicate.slopboard.
 * suggestion.llm.LlmEngine]); [indexOrNull] is non-blocking and returns `null` until the load
 * finishes, so the first keystrokes after process start fall through to empty instant chips.
 *
 * The asset is `word<space>frequency` per line (lowercase words). It ships in the APK, so unlike the
 * LLM the dictionary is always available offline.
 */
@Singleton
class DictionaryEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationCoroutineScope private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : Dictionary {
    @Volatile
    private var index: WordIndex? = null

    private val warmUp: Unit by lazy {
        scope.launch(dispatcherProvider.io) { index = load() }
        Unit
    }

    override fun indexOrNull(): WordIndex? {
        warmUp
        return index
    }

    private fun load(): WordIndex? = runCatching {
        val frequencies = HashMap<String, Long>(EXPECTED_WORDS)
        context.assets.open(ASSET_PATH).bufferedReader().useLines { lines ->
            for (raw in lines) {
                val line = raw.removePrefix(BYTE_ORDER_MARK)
                val space = line.indexOf(' ')
                if (space <= 0) continue
                val word = line.substring(0, space).lowercase()
                val frequency = line.substring(space + 1).trim().toLongOrNull() ?: continue
                frequencies[word] = frequency
            }
        }
        logDebug { "Dictionary loaded: ${frequencies.size} words" }
        WordIndex(frequencies)
    }.getOrElse {
        logDebug { "Dictionary failed to load: ${it.message}" }
        null
    }

    private companion object {
        const val ASSET_PATH = "dictionary/en_frequency.txt"
        const val EXPECTED_WORDS = 90_000
        const val BYTE_ORDER_MARK = "﻿"
    }
}
