package com.ragkit.core.storage

import com.ragkit.core.model.RagSearchResult
import com.ragkit.core.model.RagStats
import com.ragkit.core.model.TextChunk
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the persistence and vector retrieval contract for RagKit.
 *
 * Implementations (such as Room with cosine similarity) store documents, text chunks,
 * and high-dimensional vector embeddings, and perform nearest-neighbor or similarity searches.
 */
interface RagStorage {

    /**
     * Initializes the storage layer, opening databases and executing schema migrations.
     *
     * @return [Result.success] on successful initialization, or [Result.failure] on failure.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Persists text chunks alongside their corresponding vector embeddings.
     *
     * @param chunks List of text chunks to be saved.
     * @param embeddings List of vector embeddings corresponding one-to-one with [chunks].
     * @return [Result.success] if saved successfully, or [Result.failure] on database error.
     */
    suspend fun insertChunks(chunks: List<TextChunk>, embeddings: List<FloatArray>): Result<Unit>

    /**
     * Deletes all text chunks and metadata associated with a given document ID.
     *
     * @param documentId Unique identifier of the document to delete.
     * @return [Result.success] on deletion, or [Result.failure] on database error.
     */
    suspend fun deleteByDocumentId(documentId: String): Result<Unit>

    /**
     * Deletes all documents and chunks tagged with a specific source category.
     *
     * @param source The source tag/category identifier.
     * @return [Result.success] on deletion, or [Result.failure] on database error.
     */
    suspend fun deleteBySource(source: String): Result<Unit>

    /**
     * Performs a nearest-neighbor vector similarity search comparing [queryEmbedding]
     * against stored chunk embeddings.
     *
     * @param queryEmbedding The vector representation of the search query.
     * @param limit Maximum number of top-ranking results to return.
     * @param minScore Minimum similarity score threshold (0.0 to 1.0).
     * @return [Result.success] containing sorted list of [RagSearchResult], or [Result.failure].
     */
    suspend fun search(queryEmbedding: FloatArray, limit: Int, minScore: Float): Result<List<RagSearchResult>>

    /**
     * Fetches aggregated statistics regarding indexed documents, chunks, and storage size.
     *
     * @return [Result.success] containing [RagStats], or [Result.failure].
     */
    suspend fun getStats(): Result<RagStats>

    /**
     * Purges all documents, chunks, and embeddings from the storage.
     *
     * @return [Result.success] on successful clear, or [Result.failure].
     */
    suspend fun clear(): Result<Unit>

    /**
     * Observes real-time changes to the storage (inserts, updates, deletions).
     * Emits a Unit event whenever data changes.
     *
     * @return [Flow] emitting notifications of storage mutations.
     */
    fun observeChanges(): Flow<Unit>

    /**
     * Gracefully closes database connections and frees resources.
     */
    suspend fun close()
}
