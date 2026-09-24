package com.ragkit.core.model

import java.util.UUID

/**
 * Represents a document to be indexed and searched within RagKit.
 *
 * @property id Unique identifier of the document. Defaults to a randomly generated UUID.
 * @property text The raw text content of the document.
 * @property source Optional category, origin, or group tag (e.g. "notes", "chat", "email").
 * @property metadata Arbitrary key-value metadata attached to the document.
 * @property createdAt Timestamp when the document was created in milliseconds.
 * @property updatedAt Timestamp when the document was last updated in milliseconds.
 */
data class RagDocument(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val source: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "Document ID cannot be blank." }
        require(text.isNotBlank()) { "Document text cannot be blank." }
    }
}
