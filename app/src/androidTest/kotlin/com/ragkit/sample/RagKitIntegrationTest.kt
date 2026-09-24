package com.ragkit.sample

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ragkit.core.RagKit
import com.ragkit.core.chunker.SimpleTextChunker
import com.ragkit.core.engine.EmbeddingEngine
import com.ragkit.core.model.RagDocument
import com.ragkit.storage.room.RoomRagStorage
import com.ragkit.storage.room.database.RagDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RagKitIntegrationTest {

    private lateinit var ragKit: RagKit
    private lateinit var storage: RoomRagStorage

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(context, RagDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = RoomRagStorage(context, inMemoryDb)

        val deterministicEngine = object : EmbeddingEngine {
            override suspend fun initialize(): Result<Unit> = Result.success(Unit)
            override suspend fun embed(text: String): Result<FloatArray> {
                // Produces vector proportional to string length for deterministic testing
                val v = text.length.toFloat()
                return Result.success(floatArrayOf(v, 1.0f))
            }
            override suspend fun embedBatch(texts: List<String>): Result<List<FloatArray>> {
                return Result.success(texts.map { floatArrayOf(it.length.toFloat(), 1.0f) })
            }
            override fun isReady(): Boolean = true
            override fun getModelName(): String = "TestDeterministicEngine"
            override fun getModelSizeBytes(): Long = 512L
            override fun release() {}
        }

        ragKit = RagKit.builder(context)
            .embeddingEngine(deterministicEngine)
            .storage(storage)
            .chunker(SimpleTextChunker(maxChars = 200, overlap = 20))
            .build()
    }

    @After
    fun tearDown() = runBlocking {
        ragKit.clear()
        storage.close()
    }

    @Test
    fun endToEnd_indexingAndSemanticSearch_returnsMatch() = runBlocking {
        val note = RagDocument(
            id = "integration-note-1",
            text = "Yarın sabah strateji toplantısı yapılacak.",
            source = "toplantı"
        )

        val indexResult = ragKit.index(note)
        assertTrue(indexResult.isSuccess)

        val searchResult = ragKit.search("toplantı planı", limit = 5)
        assertTrue(searchResult.isSuccess)

        val results = searchResult.getOrThrow()
        assertEquals(1, results.size)
        assertEquals("integration-note-1", results[0].documentId)
        assertTrue(results[0].score > 0.0f)
    }
}
