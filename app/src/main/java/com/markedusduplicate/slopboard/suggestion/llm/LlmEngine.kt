package com.markedusduplicate.slopboard.suggestion.llm

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the LiteRT-LM [Engine] for the app session: resolves the model file, initialises the engine
 * once (trying NPU → GPU → CPU), and keeps it alive.
 *
 * The model is loaded from the first `.litertlm` file in the app's external `models/` directory
 * (push one with `adb push … /Android/data/<pkg>/files/models/`). If no model is present — or every
 * backend fails to initialise — [engineOrNull] returns `null` and the keyboard falls back to its
 * on-device n-gram suggestions.
 */
@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val mutex = Mutex()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var initFailed = false

    suspend fun engineOrNull(): Engine? {
        engine?.let { return it }
        val modelPath = resolveModelPath() ?: return null
        if (initFailed) return null

        return mutex.withLock {
            engine ?: run {
                if (initFailed) return@run null
                val created = withContext(dispatcherProvider.io) { initialize(modelPath) }
                if (created == null) initFailed = true else engine = created
                created
            }
        }
    }

    fun close() {
        engine?.close()
        engine = null
    }

    private fun initialize(modelPath: String): Engine? {
        for (backend in backends()) {
            try {
                val candidate = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                        cacheDir = context.cacheDir.absolutePath,
                    ),
                )
                candidate.initialize()
                logDebug { "LiteRT engine initialised on $backend" }
                return candidate
            } catch (t: Throwable) {
                logDebug { "LiteRT backend $backend failed: ${t.message}" }
            }
        }
        return null
    }

    private fun backends(): List<Backend> = listOf(
        Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
        Backend.GPU(),
        Backend.CPU(),
    )

    private fun resolveModelPath(): String? =
        context.getExternalFilesDir(MODELS_DIR)
            ?.listFiles { file -> file.isFile && file.name.endsWith(MODEL_EXTENSION) }
            ?.firstOrNull()
            ?.absolutePath

    private companion object {
        const val MODELS_DIR = "models"
        const val MODEL_EXTENSION = ".litertlm"
    }
}
