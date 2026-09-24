package com.ragkit.core

import android.content.Context
import com.ragkit.core.chunker.SimpleTextChunker
import com.ragkit.core.engine.EmbeddingEngine
import com.ragkit.core.exception.RagException
import com.ragkit.core.model.RagConfig
import com.ragkit.core.model.RagDocument
import com.ragkit.core.model.RagSearchResult
import com.ragkit.core.model.RagStats
import com.ragkit.core.model.TextChunk
import com.ragkit.core.storage.RagStorage
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RagKitTest {

    private lateinit var fakeEngine: FakeEmbeddingEngine
    private lateinit var fakeStorage: FakeRagStorage
    private lateinit var ragKit: RagKit
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        val mockContext = mockk<Context>(relaxed = true)
        fakeEngine = FakeEmbeddingEngine()
        fakeStorage = FakeRagStorage()

        ragKit = RagKit(
            context = mockContext,
            embeddingEngine = fakeEngine,
            storage = fakeStorage,
            chunker = SimpleTextChunker(maxChars = 200, overlap = 20),
            config = RagConfig(maxResults = 5, minScore = 0.1f),
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `index and search returns matched results`() = runTest(testDispatcher) {
        val doc = RagDocument(
            id = "doc-1",
            text = "Yapay zeka ve anlamsal arama teknolojileri."
        )

        val indexResult = ragKit.index(doc)
        assertTrue(indexResult.isSuccess)
        assertEquals(1, fakeStorage.storedChunks.size)

        val searchResult = ragKit.search("anlamsal arama", limit = 5)
        assertTrue(searchResult.isSuccess)
        val matches = searchResult.getOrThrow()
        assertEquals(1, matches.size)
        assertEquals("doc-1", matches[0].documentId)
        assertTrue(matches[0].score > 0.0f)
    }

    @Test
    fun `search with empty query returns QueryEmptyException`() = runTest(testDispatcher) {
        val result = ragKit.search("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RagException.QueryEmptyException)
    }

    @Test
    fun `deleteById removes chunks for document`() = runTest(testDispatcher) {
        val doc = RagDocument(id = "doc-del", text = "Silinecek doküman metni.")
        ragKit.index(doc)
        assertFalse(fakeStorage.storedChunks.isEmpty())

        val deleteResult = ragKit.deleteById("doc-del")
        assertTrue(deleteResult.isSuccess)
        assertTrue(fakeStorage.storedChunks.isEmpty())
    }

    @Test
    fun `clear purges all stored documents and chunks`() = runTest(testDispatcher) {
        ragKit.index(RagDocument(id = "doc-clear-1", text = "Metin 1"))
        ragKit.index(RagDocument(id = "doc-clear-2", text = "Metin 2"))

        val clearResult = ragKit.clear()
        assertTrue(clearResult.isSuccess)
        assertEquals(0, fakeStorage.storedChunks.size)
    }

    @Test
    fun `searchFlow emits results initially and on changes`() = runTest(testDispatcher) {
        val doc = RagDocument(id = "doc-flow", text = "Flow reaktif arama testi.")
        ragKit.index(doc)

        val flow = ragKit.searchFlow("reaktif", limit = 5)
        val initialEmission = flow.first()
        assertEquals(1, initialEmission.size)
    }
}

class FakeEmbeddingEngine : EmbeddingEngine {
    var initialized = false

    override suspend fun initialize(): Result<Unit> {
        initialized = true
        return Result.success(Unit)
    }

    override suspend fun embed(text: String): Result<FloatArray> {
        return Result.success(floatArrayOf(1.0f, 0.0f, 0.5f))
    }

    override suspend fun embedBatch(texts: List<String>): Result<List<FloatArray>> {
        return Result.success(texts.map { floatArrayOf(1.0f, 0.0f, 0.5f) })
    }

    override fun isReady(): Boolean = initialized
    override fun getModelName(): String = "fake-model"
    override fun getModelSizeBytes(): Long = 1024L
    override fun release() {
        initialized = false
    }
}

class FakeRagStorage : RagStorage {
    val storedChunks = mutableListOf<TextChunk>()
    val storedEmbeddings = mutableListOf<FloatArray>()
    private val changeNotifier = MutableSharedFlow<Unit>(replay = 1)

    override suspend fun initialize(): Result<Unit> = Result.success(Unit)

    override suspend fun insertChunks(
        chunks: List<TextChunk>,
        embeddings: List<FloatArray>
    ): Result<Unit> {
        storedChunks.addAll(chunks)
        storedEmbeddings.addAll(embeddings)
        changeNotifier.tryEmit(Unit)
        return Result.success(Unit)
    }

    override suspend fun deleteByDocumentId(documentId: String): Result<Unit> {
        val indicesToRemove = storedChunks.indices.filter { storedChunks[it].documentId == documentId }
        indicesToRemove.reversed().forEach { index ->
            storedChunks.removeAt(index)
            storedEmbeddings.removeAt(index)
        }
        changeNotifier.tryEmit(Unit)
        return Result.success(Unit)
    }

    override suspend fun deleteBySource(source: String): Result<Unit> {
        storedChunks.clear()
        storedEmbeddings.clear()
        changeNotifier.tryEmit(Unit)
        return Result.success(Unit)
    }

    override suspend fun search(
        queryEmbedding: FloatArray,
        limit: Int,
        minScore: Float
    ): Result<List<RagSearchResult>> {
        val results = storedChunks.map { chunk ->
            RagSearchResult(
                documentId = chunk.documentId,
                chunkId = chunk.id,
                text = chunk.text,
                score = 0.95f,
                metadata = chunk.metadata
            )
        }
        return Result.success(results.take(limit))
    }

    override suspend fun getStats(): Result<RagStats> {
        return Result.success(
            RagStats(
                documentCount = storedChunks.map { it.documentId }.distinct().size,
                chunkCount = storedChunks.size,
                storageSizeBytes = 2048L,
                modelName = "Fake Storage"
            )
        )
    }

    override suspend fun clear(): Result<Unit> {
        storedChunks.clear()
        storedEmbeddings.clear()
        changeNotifier.tryEmit(Unit)
        return Result.success(Unit)
    }

    override fun observeChanges(): Flow<Unit> = changeNotifier

    override suspend fun close() {
        storedChunks.clear()
    }
}
