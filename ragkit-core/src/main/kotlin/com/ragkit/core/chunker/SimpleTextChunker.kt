package com.ragkit.core.chunker

import com.ragkit.core.model.RagConfig
import com.ragkit.core.model.RagDocument
import com.ragkit.core.model.TextChunk
import java.util.UUID

/**
 * Default implementation of [TextChunker] that breaks down documents into semantically coherent chunks.
 *
 * Algorithm hierarchy:
 * 1. Splits text by paragraph boundaries (`\n\n`)
 * 2. If a paragraph exceeds [maxChars], splits by sentence boundaries (`.`, `!`, `?`)
 * 3. If a sentence exceeds [maxChars], falls back to fixed character windows with [overlap]
 * 4. Filters out empty or blank chunks
 * 5. Merges very short chunks (< 20 characters) with the immediately preceding chunk to preserve context
 *
 * @property maxChars Maximum character length permitted per chunk. Defaults to [RagConfig.DEFAULT_MAX_CHUNK_SIZE] (500).
 * @property overlap Character overlap maintained across consecutive chunk windows. Defaults to [RagConfig.DEFAULT_CHUNK_OVERLAP] (50).
 */
class SimpleTextChunker(
    val maxChars: Int = RagConfig.DEFAULT_MAX_CHUNK_SIZE,
    val overlap: Int = RagConfig.DEFAULT_CHUNK_OVERLAP
) : TextChunker {

    init {
        require(maxChars > 0) { "maxChars must be greater than 0. Given: $maxChars" }
        require(overlap >= 0) { "overlap cannot be negative. Given: $overlap" }
        require(overlap < maxChars) {
            "overlap ($overlap) must be strictly less than maxChars ($maxChars)."
        }
    }

    override fun chunk(document: RagDocument): List<TextChunk> {
        val rawText = document.text.trim()
        if (rawText.isBlank()) return emptyList()

        // Short-circuit: text already fits within maxChars
        if (rawText.length <= maxChars) {
            return listOf(
                TextChunk(
                    id = UUID.randomUUID().toString(),
                    documentId = document.id,
                    text = rawText,
                    index = 0,
                    metadata = document.metadata
                )
            )
        }

        val rawChunks = mutableListOf<String>()
        val paragraphs = rawText.split(Regex("(\\r?\\n){2,}")).map { it.trim() }.filter { it.isNotBlank() }

        if (paragraphs.size > 1) {
            var currentBuffer = StringBuilder()
            for (paragraph in paragraphs) {
                if (currentBuffer.isEmpty()) {
                    if (paragraph.length <= maxChars) {
                        currentBuffer.append(paragraph)
                    } else {
                        rawChunks.addAll(splitBySentencesOrChars(paragraph))
                    }
                } else {
                    if (currentBuffer.length + 2 + paragraph.length <= maxChars) {
                        currentBuffer.append("\n\n").append(paragraph)
                    } else {
                        rawChunks.add(currentBuffer.toString())
                        currentBuffer = StringBuilder()
                        if (paragraph.length <= maxChars) {
                            currentBuffer.append(paragraph)
                        } else {
                            rawChunks.addAll(splitBySentencesOrChars(paragraph))
                        }
                    }
                }
            }
            if (currentBuffer.isNotEmpty()) {
                rawChunks.add(currentBuffer.toString())
            }
        } else {
            rawChunks.addAll(splitBySentencesOrChars(rawText))
        }

        // Merge very short chunks (< 20 characters) with the previous chunk if possible
        val mergedChunks = mutableListOf<String>()
        for (chunk in rawChunks) {
            val trimmed = chunk.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.length < MIN_CHUNK_CHARS && mergedChunks.isNotEmpty()) {
                val lastIndex = mergedChunks.lastIndex
                val previous = mergedChunks[lastIndex]
                if (previous.length + 1 + trimmed.length <= maxChars + overlap) {
                    mergedChunks[lastIndex] = "$previous $trimmed"
                } else {
                    mergedChunks.add(trimmed)
                }
            } else {
                mergedChunks.add(trimmed)
            }
        }

        return mergedChunks.mapIndexed { index, text ->
            TextChunk(
                id = UUID.randomUUID().toString(),
                documentId = document.id,
                text = text,
                index = index,
                metadata = document.metadata
            )
        }
    }

    private fun splitBySentencesOrChars(text: String): List<String> {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (sentences.size > 1) {
            val result = mutableListOf<String>()
            var currentBuffer = StringBuilder()

            for (sentence in sentences) {
                if (currentBuffer.isEmpty()) {
                    if (sentence.length <= maxChars) {
                        currentBuffer.append(sentence)
                    } else {
                        result.addAll(splitByCharacterOverlap(sentence))
                    }
                } else {
                    if (currentBuffer.length + 1 + sentence.length <= maxChars) {
                        currentBuffer.append(" ").append(sentence)
                    } else {
                        result.add(currentBuffer.toString())
                        currentBuffer = StringBuilder()
                        if (sentence.length <= maxChars) {
                            currentBuffer.append(sentence)
                        } else {
                            result.addAll(splitByCharacterOverlap(sentence))
                        }
                    }
                }
            }
            if (currentBuffer.isNotEmpty()) {
                result.add(currentBuffer.toString())
            }
            return result
        }

        return splitByCharacterOverlap(text)
    }

    private fun splitByCharacterOverlap(text: String): List<String> {
        val chunks = mutableListOf<String>()
        val step = maxChars - overlap
        var start = 0
        while (start < text.length) {
            val end = (start + maxChars).coerceAtMost(text.length)
            val segment = text.substring(start, end).trim()
            if (segment.isNotBlank()) {
                chunks.add(segment)
            }
            if (end == text.length) break
            start += step
        }
        return chunks
    }

    companion object {
        private const val MIN_CHUNK_CHARS = 20
    }
}
