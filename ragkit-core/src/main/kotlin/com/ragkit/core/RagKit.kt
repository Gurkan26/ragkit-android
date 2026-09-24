package com.ragkit.core

import android.content.Context
import com.ragkit.core.chunker.TextChunker
import com.ragkit.core.engine.EmbeddingEngine
import com.ragkit.core.exception.RagException
import com.ragkit.core.model.RagConfig
import com.ragkit.core.model.RagDocument
import com.ragkit.core.model.RagSearchResult
import com.ragkit.core.model.RagStats
import com.ragkit.core.storage.RagStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Main entry point for the RagKit On-Device Semantic Search SDK.
 *
 * Provides high-level APIs for document chunking, vector embedding, persistent Room storage,
 * and cosine similarity nearest-neighbor semantic search.
 */
class RagKit internal constructor(
    @Suppress("unused")
    private val context: Context,
    private val embeddingEngine: EmbeddingEngine,
    private val storage: RagStorage,
    private val chunker: TextChunker,
    val config: RagConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val operationMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * Initializes the underlying storage and embedding model.
     *
     * @return [Result.success] if ready, or [Result.failure] on error.
     */
    suspend fun initialize(): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            runCatching {
                val storageResult = storage.initialize()
                if (storageResult.isFailure) {
                    throw storageResult.exceptionOrNull()
                        ?: RagException.InitializationException("Failed to initialize storage.")
                }

                val engineResult = embeddingEngine.initialize()
                if (engineResult.isFailure) {
                    throw engineResult.exceptionOrNull()
                        ?: RagException.InitializationException("Failed to initialize embedding engine.")
                }
            }
        }
    }

    /**
     * Indexes a single [RagDocument] by chunking its text, calculating vector embeddings,
     * and saving chunks and vectors into storage.
     *
     * @param document The document to chunk and index.
     * @return [Result.success] when fully indexed, or [Result.failure] on error.
     */
    suspend fun index(document: RagDocument): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            runCatching {
                if (!embeddingEngine.isReady()) {
                    val initResult = embeddingEngine.initialize()
                    if (initResult.isFailure) {
                        throw initResult.exceptionOrNull()
                            ?: RagException.ModelNotReadyException()
                    }
                }

                val chunks = chunker.chunk(document)
                if (chunks.isEmpty()) return@runCatching

                val texts = chunks.map { it.text }
                val embeddingResult = embeddingEngine.embedBatch(texts)
                val embeddings = embeddingResult.getOrThrow()

                val insertResult = storage.insertChunks(chunks, embeddings)
                insertResult.getOrThrow()
            }
        }
    }

    /**
     * Indexes a batch of [RagDocument] instances in a single atomic operation.
     *
     * @param documents The list of documents to chunk and index.
     * @return [Result.success] when all documents are indexed, or [Result.failure] on error.
     */
    suspend fun index(documents: List<RagDocument>): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            runCatching {
                if (documents.isEmpty()) return@runCatching

                if (!embeddingEngine.isReady()) {
                    val initResult = embeddingEngine.initialize()
                    if (initResult.isFailure) {
                        throw initResult.exceptionOrNull()
                            ?: RagException.ModelNotReadyException()
                    }
                }

                val allChunks = documents.flatMap { chunker.chunk(it) }
                if (allChunks.isEmpty()) return@runCatching

                val texts = allChunks.map { it.text }
                val embeddings = embeddingEngine.embedBatch(texts).getOrThrow()

                storage.insertChunks(allChunks, embeddings).getOrThrow()
            }
        }
    }

    /**
     * Searches indexed text chunks semantically by computing the cosine similarity between
     * the query embedding and stored chunk vectors.
     *
     * @param query The natural language search query.
     * @param limit The maximum number of results to return. Defaults to [RagConfig.maxResults].
     * @return [Result.success] containing ordered [RagSearchResult] list, or [Result.failure].
     */
    suspend fun search(
        query: String,
        limit: Int = config.maxResults
    ): Result<List<RagSearchResult>> = withContext(ioDispatcher) {
        runCatching {
            if (query.trim().isEmpty()) {
                throw RagException.QueryEmptyException()
            }

            if (!embeddingEngine.isReady()) {
                val initResult = embeddingEngine.initialize()
                if (initResult.isFailure) {
                    throw initResult.exceptionOrNull()
                        ?: RagException.ModelNotReadyException()
                }
            }

            val queryEmbedding = embeddingEngine.embed(query).getOrThrow()
            storage.search(queryEmbedding, limit, config.minScore).getOrThrow()
        }
    }

    /**
     * Returns a cold [Flow] that executes semantic search for the given [query] initially,
     * and automatically re-executes search whenever stored data changes.
     *
     * @param query The natural language search query.
     * @param limit The maximum number of results to return. Defaults to [RagConfig.maxResults].
     * @return [Flow] emitting the updated search results.
     */
    fun searchFlow(
        query: String,
        limit: Int = config.maxResults
    ): Flow<List<RagSearchResult>> = flow {
        emitAll(
            storage.observeChanges()
                .onStart { emit(Unit) }
                .map {
                    if (query.isBlank()) {
                        emptyList()
                    } else {
                        search(query, limit).getOrDefault(emptyList())
                    }
                }
        )
    }.flowOn(ioDispatcher)

    /**
     * Deletes all chunks, embeddings, and metadata corresponding to a specific document ID.
     *
     * @param documentId The unique ID of the document to delete.
     * @return [Result.success] on deletion, or [Result.failure] on error.
     */
    suspend fun deleteById(documentId: String): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            storage.deleteByDocumentId(documentId)
        }
    }

    /**
     * Deletes all documents, chunks, and embeddings matching a given source tag/category.
     *
     * @param source The source category identifier.
     * @return [Result.success] on deletion, or [Result.failure] on error.
     */
    suspend fun deleteBySource(source: String): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            storage.deleteBySource(source)
        }
    }

    /**
     * Retrieves aggregated statistics about documents, chunks, model, and database size.
     *
     * @return [Result.success] with [RagStats], or [Result.failure].
     */
    suspend fun getStats(): Result<RagStats> = withContext(ioDispatcher) {
        storage.getStats()
    }

    /**
     * Clears all indexed documents, chunks, and embeddings from the storage.
     *
     * @return [Result.success] on clear, or [Result.failure].
     */
    suspend fun clear(): Result<Unit> = withContext(ioDispatcher) {
        operationMutex.withLock {
            storage.clear()
        }
    }

    /**
     * Releases active embedding model resources and database connections.
     */
    fun close() {
        embeddingEngine.release()
        scope.cancel()
    }

    companion object {
        /**
         * Creates a new [RagKitBuilder] to configure and build a [RagKit] instance.
         */
        fun builder(context: Context): RagKitBuilder = RagKitBuilder(context)

        /**
         * Convenience DSL function to instantiate and configure [RagKit].
         */
        inline fun create(context: Context, block: RagKitBuilder.() -> Unit): RagKit {
            return RagKitBuilder(context).apply(block).build()
        }
    }
}
