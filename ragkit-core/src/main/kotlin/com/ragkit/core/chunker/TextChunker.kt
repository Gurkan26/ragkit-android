package com.ragkit.core.chunker

import com.ragkit.core.model.RagDocument
import com.ragkit.core.model.TextChunk

/**
 * Interface responsible for dividing a [RagDocument] into smaller, semantically coherent [TextChunk] units.
 */
interface TextChunker {

    /**
     * Splits a document into a list of [TextChunk] instances suitable for embedding and retrieval.
     *
     * @param document The document to chunk.
     * @return List of text chunks with preserved order and metadata.
     */
    fun chunk(document: RagDocument): List<TextChunk>
}
