package com.ragkit.core

import android.content.Context
import com.ragkit.core.chunker.SimpleTextChunker
import com.ragkit.core.chunker.TextChunker
import com.ragkit.core.engine.EmbeddingEngine
import com.ragkit.core.exception.RagException
import com.ragkit.core.model.RagConfig
import com.ragkit.core.storage.RagStorage

/**
 * Fluent builder for creating configured [RagKit] instances.
 *
 * @param context Android [Context] for lifecycle, cache, and database operations.
 */
class RagKitBuilder(private val context: Context) {
    private var embeddingEngine: EmbeddingEngine? = null
    private var storage: RagStorage? = null
    private var chunker: TextChunker? = null
    private var config: RagConfig = RagConfig.DEFAULT

    /**
     * Sets the embedding engine used for text vectorization.
     */
    fun embeddingEngine(engine: EmbeddingEngine): RagKitBuilder = apply {
        this.embeddingEngine = engine
    }

    /**
     * Sets the storage engine used for persisting chunks and running vector similarity search.
     */
    fun storage(storage: RagStorage): RagKitBuilder = apply {
        this.storage = storage
    }

    /**
     * Sets the text chunker. If not set, defaults to [SimpleTextChunker].
     */
    fun chunker(chunker: TextChunker): RagKitBuilder = apply {
        this.chunker = chunker
    }

    /**
     * Sets the configuration options for RagKit.
     */
    fun config(config: RagConfig): RagKitBuilder = apply {
        this.config = config
    }

    /**
     * Configures the [RagConfig] using a DSL builder block.
     */
    fun config(block: RagConfigDsl.() -> Unit): RagKitBuilder = apply {
        val dsl = RagConfigDsl(this.config)
        dsl.block()
        this.config = dsl.build()
    }

    /**
     * Validates configuration and constructs a ready-to-use [RagKit] instance.
     *
     * @throws RagException.InitializationException if required engines are not provided.
     */
    fun build(): RagKit {
        val engine = embeddingEngine ?: throw RagException.InitializationException(
            "EmbeddingEngine must be provided. Call embeddingEngine(...) before build()."
        )
        val stor = storage ?: throw RagException.InitializationException(
            "RagStorage must be provided. Call storage(...) before build()."
        )
        val resolvedChunker = chunker ?: SimpleTextChunker(
            maxChars = config.maxChunkSize,
            overlap = config.chunkOverlap
        )

        return RagKit(
            context = context.applicationContext,
            embeddingEngine = engine,
            storage = stor,
            chunker = resolvedChunker,
            config = config
        )
    }
}

/**
 * DSL helper for convenient inline configuration of [RagConfig].
 */
class RagConfigDsl(initial: RagConfig = RagConfig.DEFAULT) {
    var maxChunkSize: Int = initial.maxChunkSize
    var chunkOverlap: Int = initial.chunkOverlap
    var maxResults: Int = initial.maxResults
    var minScore: Float = initial.minScore

    fun build(): RagConfig = RagConfig(
        maxChunkSize = maxChunkSize,
        chunkOverlap = chunkOverlap,
        maxResults = maxResults,
        minScore = minScore
    )
}
