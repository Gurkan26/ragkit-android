package com.ragkit.storage.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ragkit.storage.room.entity.ChunkEntity
import com.ragkit.storage.room.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing documents and vector chunks in the Room database.
 */
@Dao
interface RagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    /**
     * Inserts documents and their corresponding chunks in a single atomic database transaction.
     */
    @Transaction
    suspend fun insertDocumentsAndChunks(
        documents: List<DocumentEntity>,
        chunks: List<ChunkEntity>
    ) {
        insertDocuments(documents)
        insertChunks(chunks)
    }

    @Query("SELECT * FROM chunks")
    suspend fun getAllChunks(): List<ChunkEntity>

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String): Int

    @Query("DELETE FROM documents WHERE source = :source")
    suspend fun deleteDocumentsBySource(source: String): Int

    @Query("DELETE FROM chunks WHERE document_id = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: String): Int

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    @Query("SELECT COUNT(*) FROM chunks")
    suspend fun getChunkCount(): Int

    @Query("DELETE FROM chunks")
    suspend fun clearChunks(): Int

    @Query("DELETE FROM documents")
    suspend fun clearDocuments(): Int

    /**
     * Purges all chunks and documents in an atomic transaction.
     */
    @Transaction
    suspend fun clearAll() {
        clearChunks()
        clearDocuments()
    }

    /**
     * Observes changes to chunk count to signal database modifications.
     */
    @Query("SELECT COUNT(*) FROM chunks")
    fun observeChunkCount(): Flow<Int>
}
