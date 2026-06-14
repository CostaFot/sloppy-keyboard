package com.markedusduplicate.slopboard.suggestion.llm

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.di.ApplicationCoroutineScope
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the LiteRT-LM [Engine] for the app session: resolves the model file and initialises the
 * engine once (trying NPU → GPU → CPU), keeping it alive.
 *
 * Warm-up runs on the application scope so it can't be cancelled and restarted by callers.
 * [engineOrNull] is non-blocking: it returns `null` while the model is still loading (or if no model
 * is present / every backend fails), so callers (OCR screen reading, the agent) degrade gracefully
 * until the engine comes online.
 *
 * The model is loaded from the first `.litertlm` file in the app's external `models/` directory
 * (push one with `adb push … /Android/data/<pkg>/files/models/`).
 */
@Singleton
class LlmEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationCoroutineScope private val scope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) {
    @Volatile
    private var engine: Engine? = null

    private val warmUp: Unit by lazy {
        scope.launch(dispatcherProvider.io) { engine = initialize() }
        Unit
    }

    fun engineOrNull(): Engine? {
        warmUp
        return engine
    }

    /**
     * One-shot multimodal generation: sends [jpeg] (image bytes) plus [prompt] to the model and
     * returns the raw reply, or null if the engine isn't ready or inference fails. Runs on the IO
     * dispatcher, so it's safe to call from any context.
     */
    suspend fun generateWithImage(jpeg: ByteArray, prompt: String): String? =
        withContext(dispatcherProvider.io) {
            val activeEngine = engineOrNull() ?: return@withContext null
            runCatching {
                activeEngine.createConversation().use { conversation ->
                    conversation.sendMessage(
                        Contents.of(Content.ImageBytes(jpeg), Content.Text(prompt)),
                    ).toString()
                }
            }.getOrElse {
                logDebug { "vision inference failed: ${it.message}" }
                null
            }
        }

    /**
     * One-shot text generation: sends [prompt] to the model and returns the raw reply, or null if the
     * engine isn't ready or inference fails. Runs on IO, so it's safe to call from any context.
     */
    suspend fun generate(prompt: String): String? =
        withContext(dispatcherProvider.io) {
            val activeEngine = engineOrNull() ?: return@withContext null
            runCatching {
                activeEngine.createConversation().use { conversation ->
                    conversation.sendMessage(Contents.of(Content.Text(prompt))).toString()
                }
            }.getOrElse {
                logDebug { "text inference failed: ${it.message}" }
                null
            }
        }

    private fun initialize(): Engine? {
        val modelPath = resolveModelPath() ?: return null
        for (backend in backends()) {
            var candidate: Engine? = null
            try {
                candidate = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                        visionBackend = if (backend is Backend.GPU) Backend.GPU() else null,
                        audioBackend = if (backend is Backend.GPU) Backend.CPU() else null,
                        maxNumTokens = MAX_NUM_TOKENS,
                        maxNumImages = MAX_NUM_IMAGES,
                        cacheDir = null,
                    ),
                )
                candidate.initialize()
                logDebug { "LiteRT engine initialised on $backend" }
                return candidate
            } catch (t: Throwable) {
                logDebug { "LiteRT backend $backend failed: ${t.message}" }
                runCatching { candidate?.close() }
            }
        }
        logDebug { "LiteRT: no backend could initialise the model" }
        return null
    }

    private fun backends(): List<Backend> = listOf(
        Backend.GPU(),
        Backend.CPU(numOfThreads = cpuThreads()),
        Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
    )

    private fun cpuThreads(): Int =
        Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_CPU_THREADS)

    private fun resolveModelPath(): String? =
        context.getExternalFilesDir(MODELS_DIR)
            ?.listFiles { file -> file.isFile && file.name.endsWith(MODEL_EXTENSION) }
            ?.firstOrNull()
            ?.absolutePath

    private companion object {
        const val MODELS_DIR = "models"
        const val MODEL_EXTENSION = ".litertlm"
        const val MAX_CPU_THREADS = 4
        const val MAX_NUM_TOKENS = 1024
        const val MAX_NUM_IMAGES = 1
    }
}
