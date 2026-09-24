package com.ragkit.storage.room

import android.content.Context
import com.ragkit.core.exception.RagException
import com.ragkit.core.model.RagSearchResult
import com.ragkit.core.model.RagStats
import com.ragkit.core.model.TextChunk
import com.ragkit.core.storage.RagStorage
import com.ragkit.storage.room.converter.Converters
import com.ragkit.storage.room.database.RagDatabase
import com.ragkit.storage.room.entity.ChunkEntity
import com.ragkit.storage.room.entity.DocumentEntity
import com.ragkit.storage.room.util.CosineSimilarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Room-based implementation of [RagStorage] using brute-force cosine similarity vector search.
 *
 * @param context Android [Context] for database creation and storage metrics.
 * @param database Optional custom [RagDatabase] instance (useful for in-memory testing).
 */
class RoomRagStorage(
    private val context: Context,
    private val database: RagDatabase = RagDatabase.getInstance(context)
) : RagStorage {

    private val dao = database.ragDao()
    private val converters = Converters()

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Verify database can be queried
            dao.getChunkCount()
            Unit
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to initialize Room database: ${error.message}", error)
        }
    }

    override suspend fun insertChunks(
        chunks: List<TextChunk>,
        embeddings: List<FloatArray>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(chunks.size == embeddings.size) {
                "Chunks count (${chunks.size}) must match embeddings count (${embeddings.size})."
            }
            if (chunks.isEmpty()) return@runCatching

            val now = System.currentTimeMillis()
            val distinctDocs = chunks.map { it.documentId }.distinct().map { docId ->
                val sampleChunk = chunks.first { it.documentId == docId }
                DocumentEntity(
                    id = docId,
                    source = sampleChunk.metadata["source"],
                    metadata = converters.fromMetadataMap(sampleChunk.metadata),
                    createdAt = now,
                    updatedAt = now
                )
            }

            val chunkEntities = chunks.mapIndexed { i, chunk ->
                val embeddingBytes = converters.fromFloatArray(embeddings[i])
                    ?: throw RagException.StorageException("Failed to serialize embedding vector for chunk: ${chunk.id}")

                ChunkEntity(
                    id = chunk.id,
                    documentId = chunk.documentId,
                    text = chunk.text,
                    embedding = embeddingBytes,
                    chunkIndex = chunk.index,
                    metadata = converters.fromMetadataMap(chunk.metadata),
                    createdAt = now
                )
            }

            dao.insertDocumentsAndChunks(distinctDocs, chunkEntities)
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to insert chunks into Room: ${error.message}", error)
        }
    }

    override suspend fun deleteByDocumentId(documentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dao.deleteChunksByDocumentId(documentId)
            dao.deleteDocumentById(documentId)
            Unit
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to delete document $documentId: ${error.message}", error)
        }
    }

    override suspend fun deleteBySource(source: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dao.deleteDocumentsBySource(source)
            Unit
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to delete source $source: ${error.message}", error)
        }
    }

    override suspend fun search(
        queryEmbedding: FloatArray,
        limit: Int,
        minScore: Float
    ): Result<List<RagSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val allChunks = dao.getAllChunks()
            if (allChunks.isEmpty()) return@runCatching emptyList()

            val scoredResults = ArrayList<RagSearchResult>(allChunks.size)
            for (entity in allChunks) {
                val chunkVector = converters.toFloatArray(entity.embedding) ?: continue
                val score = CosineSimilarity.calculate(queryEmbedding, chunkVector)

                if (score >= minScore) {
                    scoredResults.add(
                        RagSearchResult(
                            documentId = entity.documentId,
                            chunkId = entity.id,
                            text = entity.text,
                            score = score,
                            metadata = converters.toMetadataMap(entity.metadata)
                        )
                    )
                }
            }

            scoredResults.sort() // Sorts descending by score via Comparable
            scoredResults.take(limit)
        }.recoverCatching { error ->
            throw RagException.StorageException("Error executing vector search: ${error.message}", error)
        }
    }

    override suspend fun getStats(): Result<RagStats> = withContext(Dispatchers.IO) {
        runCatching {
            val docCount = dao.getDocumentCount()
            val chunkCount = dao.getChunkCount()
            val dbFile = context.getDatabasePath(RagDatabase.DATABASE_NAME)
            val dbSize = if (dbFile != null && dbFile.exists()) dbFile.length() else 0L

            RagStats(
                documentCount = docCount,
                chunkCount = chunkCount,
                storageSizeBytes = dbSize,
                modelName = "Room SQLite Vector Table"
            )
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to retrieve storage stats: ${error.message}", error)
        }
    }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dao.clearAll()
        }.recoverCatching { error ->
            throw RagException.StorageException("Failed to clear storage: ${error.message}", error)
        }
    }

    override fun observeChanges(): Flow<Unit> {
        return dao.observeChunkCount()
            .distinctUntilChanged()
            .map { }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        if (database.isOpen) {
            database.close()
        }
    }
}
