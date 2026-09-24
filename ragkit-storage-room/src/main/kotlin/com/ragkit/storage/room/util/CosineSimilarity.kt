package com.ragkit.storage.room.util

import kotlin.math.sqrt

/**
 * Utility object for computing normalized cosine similarity between vector embeddings.
 */
object CosineSimilarity {

    /**
     * Calculates the cosine similarity between two float vectors and normalizes the score to `[0.0, 1.0]`.
     *
     * Cosine similarity mathematically ranges from -1.0 (opposite) to 1.0 (identical).
     * The normalized score is computed as:
     * `((rawSimilarity + 1.0) / 2.0).coerceIn(0.0f, 1.0f)`
     *
     * Special cases:
     * - Returns `0.0f` if either vector is empty or dimensions do not match.
     * - Returns `0.0f` if either vector is a zero vector (magnitude = 0).
     *
     * @param a First float vector.
     * @param b Second float vector.
     * @return Normalized similarity score between 0.0f and 1.0f.
     */
    fun calculate(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) {
            return 0.0f
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            val vA = a[i].toDouble()
            val vB = b[i].toDouble()
            dotProduct += vA * vB
            normA += vA * vA
            normB += vB * vB
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0f
        }

        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator == 0.0) {
            return 0.0f
        }

        val rawSimilarity = (dotProduct / denominator).coerceIn(-1.0, 1.0)
        val normalized = (rawSimilarity + 1.0) / 2.0
        return normalized.toFloat().coerceIn(0.0f, 1.0f)
    }
}
