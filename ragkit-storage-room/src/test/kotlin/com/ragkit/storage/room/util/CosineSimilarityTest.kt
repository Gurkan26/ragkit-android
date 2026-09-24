package com.ragkit.storage.room.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CosineSimilarityTest {

    @Test
    fun `identical vectors return similarity of 1_0`() {
        val a = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val b = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)

        val similarity = CosineSimilarity.calculate(a, b)
        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun `opposite vectors return similarity of 0_0`() {
        val a = floatArrayOf(1.0f, 0.0f)
        val b = floatArrayOf(-1.0f, 0.0f)

        val similarity = CosineSimilarity.calculate(a, b)
        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun `orthogonal vectors return normalized similarity of 0_5`() {
        val a = floatArrayOf(1.0f, 0.0f)
        val b = floatArrayOf(0.0f, 1.0f)

        val similarity = CosineSimilarity.calculate(a, b)
        assertEquals(0.5f, similarity, 0.0001f)
    }

    @Test
    fun `zero vector returns similarity of 0_0`() {
        val a = floatArrayOf(0.0f, 0.0f, 0.0f)
        val b = floatArrayOf(1.0f, 2.0f, 3.0f)

        val similarity = CosineSimilarity.calculate(a, b)
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `empty vectors or mismatched dimensions return 0_0`() {
        assertEquals(0.0f, CosineSimilarity.calculate(floatArrayOf(), floatArrayOf(1.0f)))
        assertEquals(0.0f, CosineSimilarity.calculate(floatArrayOf(1.0f, 2.0f), floatArrayOf(1.0f)))
    }
}
