package com.ragkit.core.chunker

import com.ragkit.core.model.RagDocument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SimpleTextChunkerTest {

    private lateinit var chunker: SimpleTextChunker

    @BeforeEach
    fun setUp() {
        chunker = SimpleTextChunker(maxChars = 80, overlap = 15)
    }

    @Test
    fun `creating RagDocument with blank text throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            RagDocument(text = "")
        }
        assertThrows<IllegalArgumentException> {
            RagDocument(text = "   \n\t  ")
        }
    }

    @Test
    fun `chunk with short text under maxChars returns single chunk`() {
        val doc = RagDocument(text = "Kısa bir not.")
        val chunks = chunker.chunk(doc)

        assertEquals(1, chunks.size)
        assertEquals(doc.id, chunks[0].documentId)
        assertEquals("Kısa bir not.", chunks[0].text)
        assertEquals(0, chunks[0].index)
    }

    @Test
    fun `chunk with long text splits into multiple chunks with overlap`() {
        val longText = "Bu birinci cümledir. Bu ikinci oldukça uzun bir cümledir ve chunk limitini aşacaktır. " +
                "Bu üçüncü cümledir ve yeni bir parçaya bölünmelidir. Bu dördüncü cümledir."
        val doc = RagDocument(text = longText)
        val chunks = chunker.chunk(doc)

        assertTrue(chunks.size > 1)
        chunks.forEachIndexed { idx, chunk ->
            assertEquals(idx, chunk.index)
            assertEquals(doc.id, chunk.documentId)
            assertTrue(chunk.text.isNotBlank())
        }
    }

    @Test
    fun `chunk with paragraphs splits along double newlines when exceeding maxChars`() {
        val text = "İlk paragraf burada yer alıyor ve bu paragraf tek başına oldukça detaylı bilgiler içeriyor.\n\n" +
                "İkinci paragraf ise burada başlıyor ve devam ediyor, toplamda seksen karakter sınırını kolayca aşıyor."
        val doc = RagDocument(text = text)
        val chunks = chunker.chunk(doc)

        assertTrue(chunks.size >= 2)
    }

    @Test
    fun `invalid arguments throw IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            SimpleTextChunker(maxChars = -10, overlap = 10)
        }
        assertThrows<IllegalArgumentException> {
            SimpleTextChunker(maxChars = 50, overlap = 50)
        }
    }
}
