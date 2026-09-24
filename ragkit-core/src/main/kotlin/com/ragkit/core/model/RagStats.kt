package com.ragkit.core.model

/**
 * Aggregated statistics about the current RagKit index, storage, and active embedding model.
 *
 * @property documentCount Total number of indexed documents.
 * @property chunkCount Total number of stored vector chunks.
 * @property storageSizeBytes Approximate size of the underlying database/storage in bytes.
 * @property modelName Identifier or filename of the active embedding model.
 */
data class RagStats(
    val documentCount: Int,
    val chunkCount: Int,
    val storageSizeBytes: Long,
    val modelName: String
) {
    init {
        require(documentCount >= 0) { "documentCount cannot be negative." }
        require(chunkCount >= 0) { "chunkCount cannot be negative." }
        require(storageSizeBytes >= 0L) { "storageSizeBytes cannot be negative." }
        require(modelName.isNotBlank()) { "modelName cannot be blank." }
    }
}
