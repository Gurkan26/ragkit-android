package com.ragkit.core.model

/**
 * Configuration parameters governing text chunking, storage, and semantic search thresholds.
 *
 * @property maxChunkSize Maximum character length for each text chunk. Default is 500 characters.
 * @property chunkOverlap Number of overlapping characters between consecutive chunks to preserve semantic context. Default is 50.
 * @property maxResults Maximum number of search results returned per query. Default is 10.
 * @property minScore Minimum cosine similarity threshold (0.0 to 1.0) for a chunk to be included in results. Default is 0.3f.
 */
data class RagConfig(
    val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE,
    val chunkOverlap: Int = DEFAULT_CHUNK_OVERLAP,
    val maxResults: Int = DEFAULT_MAX_RESULTS,
    val minScore: Float = DEFAULT_MIN_SCORE
) {
    init {
        require(maxChunkSize > 0) { "maxChunkSize must be greater than 0." }
        require(chunkOverlap >= 0) { "chunkOverlap cannot be negative." }
        require(chunkOverlap < maxChunkSize) {
            "chunkOverlap ($chunkOverlap) must be strictly less than maxChunkSize ($maxChunkSize)."
        }
        require(maxResults > 0) { "maxResults must be greater than 0." }
        require(minScore in 0.0f..1.0f) { "minScore must be between 0.0 and 1.0. Given: $minScore" }
    }

    companion object {
        const val DEFAULT_MAX_CHUNK_SIZE: Int = 500
        const val DEFAULT_CHUNK_OVERLAP: Int = 50
        const val DEFAULT_MAX_RESULTS: Int = 10
        const val DEFAULT_MIN_SCORE: Float = 0.3f

        /**
         * Default configuration instance.
         */
        val DEFAULT: RagConfig = RagConfig()
    }
}
