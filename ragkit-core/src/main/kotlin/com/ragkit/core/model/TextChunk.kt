package com.ragkit.core.model

import java.util.UUID

/**
 * Represents a single text segment (chunk) extracted from a [RagDocument].
 *
 * @property id Unique identifier of the chunk. Defaults to a randomly generated UUID.
 * @property documentId The ID of the parent [RagDocument] to which this chunk belongs.
 * @property text The slice of text content.
 * @property index The zero-based positional order of this chunk within the parent document.
 * @property metadata Key-value metadata associated with this chunk.
 */
data class TextChunk(
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val text: String,
    val index: Int,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Chunk ID cannot be blank." }
        require(documentId.isNotBlank()) { "Document ID cannot be blank." }
        require(text.isNotBlank()) { "Chunk text cannot be blank." }
        require(index >= 0) { "Chunk index cannot be negative." }
    }
}
