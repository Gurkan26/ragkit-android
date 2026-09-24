package com.ragkit.core.model

/**
 * Represents a matched chunk resulting from a semantic search query.
 *
 * @property documentId The ID of the parent [RagDocument].
 * @property chunkId The ID of the specific [TextChunk] that matched the query.
 * @property text The content text of the matched chunk.
 * @property score Cosine similarity score normalized between 0.0f (no match) and 1.0f (perfect match).
 * @property metadata Key-value metadata associated with the chunk.
 */
data class RagSearchResult(
    val documentId: String,
    val chunkId: String,
    val text: String,
    val score: Float,
    val metadata: Map<String, String> = emptyMap()
) : Comparable<RagSearchResult> {
    init {
        require(score in 0.0f..1.0f) { "Score must be normalized between 0.0 and 1.0. Given: $score" }
    }

    /**
     * Compares search results in descending order of relevance score.
     */
    override fun compareTo(other: RagSearchResult): Int = other.score.compareTo(this.score)
}
