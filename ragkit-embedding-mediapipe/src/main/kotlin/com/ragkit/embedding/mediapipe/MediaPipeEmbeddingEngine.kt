package com.ragkit.embedding.mediapipe

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.ragkit.core.engine.EmbeddingEngine
import com.ragkit.core.exception.RagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device embedding engine powered by Google MediaPipe Text Embedder.
 *
 * Automatically downloads and caches models on first use, reports download progress via [downloadProgress],
 * and handles Android low-memory trimming gracefully via [ComponentCallbacks2].
 *
 * @param context Android [Context] for accessing assets and registering memory callbacks.
 * @param config Configuration detailing model parameters and download location.
 * @param downloader Model download utility.
 */
class MediaPipeEmbeddingEngine(
    private val context: Context,
    private val config: ModelConfig = ModelConfig.DEFAULT,
    private val downloader: ModelDownloader = ModelDownloader(context)
) : EmbeddingEngine, ComponentCallbacks2 {

    private val mutex = Mutex()
    private var textEmbedder: TextEmbedder? = null
    private var modelFile: File? = null

    private val _downloadProgress = MutableStateFlow(0.0f)

    /**
     * Observable download progress stream normalized between 0.0f and 1.0f.
     */
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    init {
        context.applicationContext.registerComponentCallbacks(this)
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                if (textEmbedder != null) return@runCatching

                val file = if (config.assetFilePath != null) {
                    null
                } else {
                    val downloadResult = downloader.downloadModel(config) { progress ->
                        _downloadProgress.value = progress
                    }
                    downloadResult.getOrThrow().also { modelFile = it }
                }

                val baseOptionsBuilder = BaseOptions.builder()
                if (config.assetFilePath != null) {
                    baseOptionsBuilder.setModelAssetPath(config.assetFilePath)
                } else if (file != null) {
                    baseOptionsBuilder.setModelAssetPath(file.absolutePath)
                }

                val options = TextEmbedderOptions.builder()
                    .setBaseOptions(baseOptionsBuilder.build())
                    .setQuantize(false)
                    .build()

                textEmbedder = TextEmbedder.createFromOptions(context, options)
                _downloadProgress.value = 1.0f
            }.recoverCatching { error ->
                throw if (error is RagException) error else RagException.InitializationException(
                    "Failed to initialize MediaPipe TextEmbedder: ${error.message}",
                    error
                )
            }
        }
    }

    override suspend fun embed(text: String): Result<FloatArray> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val embedder = textEmbedder ?: throw RagException.ModelNotReadyException()
                val result = embedder.embed(text)
                val embeddings = result.embeddingResult().embeddings()
                if (embeddings.isEmpty()) {
                    throw RagException.EmbeddingFailedException("No embedding returned from MediaPipe for text.")
                }
                embeddings.first().floatEmbedding()
            }.recoverCatching { error ->
                throw if (error is RagException) error else RagException.EmbeddingFailedException(
                    "Embedding generation failed: ${error.message}",
                    error
                )
            }
        }
    }

    override suspend fun embedBatch(texts: List<String>): Result<List<FloatArray>> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val embedder = textEmbedder ?: throw RagException.ModelNotReadyException()
                texts.map { text ->
                    val result = embedder.embed(text)
                    val embeddings = result.embeddingResult().embeddings()
                    if (embeddings.isEmpty()) {
                        throw RagException.EmbeddingFailedException("No embedding returned from MediaPipe for text: $text")
                    }
                    embeddings.first().floatEmbedding()
                }
            }.recoverCatching { error ->
                throw if (error is RagException) error else RagException.EmbeddingFailedException(
                    "Batch embedding generation failed: ${error.message}",
                    error
                )
            }
        }
    }

    override fun isReady(): Boolean = textEmbedder != null

    override fun getModelName(): String = config.modelName

    override fun getModelSizeBytes(): Long {
        val file = modelFile
        return if (file != null && file.exists()) file.length() else config.modelSizeBytes
    }

    override fun release() {
        textEmbedder?.close()
        textEmbedder = null
        try {
            context.applicationContext.unregisterComponentCallbacks(this)
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            textEmbedder?.close()
            textEmbedder = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith("Unit"))
    override fun onLowMemory() {
        textEmbedder?.close()
        textEmbedder = null
    }
}
