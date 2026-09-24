package com.ragkit.storage.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ragkit.core.model.TextChunk
import com.ragkit.storage.room.database.RagDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRagStorageTest {

    private lateinit var database: RagDatabase
    private lateinit var storage: RoomRagStorage

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, RagDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = RoomRagStorage(context, database)
    }

    @After
    fun tearDown() = runBlocking {
        storage.close()
    }

    @Test
    fun insertAndSearchChunks_returnsCorrectOrder() = runBlocking {
        val chunk1 = TextChunk(
            id = "c1",
            documentId = "d1",
            text = "Yapay zeka ve dil modelleri.",
            index = 0,
            metadata = mapOf("category" to "ai")
        )
        val chunk2 = TextChunk(
            id = "c2",
            documentId = "d2",
            text = "Pazardan elma ve portakal alındı.",
            index = 0,
            metadata = mapOf("category" to "market")
        )

        val embedding1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val embedding2 = floatArrayOf(0.0f, 1.0f, 0.0f)

        storage.insertChunks(listOf(chunk1, chunk2), listOf(embedding1, embedding2))

        val queryVector = floatArrayOf(0.95f, 0.05f, 0.0f)
        val searchResult = storage.search(queryVector, limit = 2, minScore = 0.5f).getOrThrow()

        assertEquals(2, searchResult.size)
        assertEquals("d1", searchResult[0].documentId)
        assertTrue(searchResult[0].score > searchResult[1].score)
    }

    @Test
    fun deleteByDocumentId_removesOnlyTargetDocument() = runBlocking {
        val chunk1 = TextChunk(id = "c1", documentId = "docA", text = "Metin A", index = 0)
        val chunk2 = TextChunk(id = "c2", documentId = "docB", text = "Metin B", index = 0)

        storage.insertChunks(listOf(chunk1, chunk2), listOf(floatArrayOf(1f), floatArrayOf(1f)))

        storage.deleteByDocumentId("docA")
        val stats = storage.getStats().getOrThrow()

        assertEquals(1, stats.documentCount)
        assertEquals(1, stats.chunkCount)
    }

    @Test
    fun clear_removesAllEntries() = runBlocking {
        val chunk = TextChunk(id = "c1", documentId = "doc1", text = "Metin", index = 0)
        storage.insertChunks(listOf(chunk), listOf(floatArrayOf(1f)))

        storage.clear()
        val stats = storage.getStats().getOrThrow()

        assertEquals(0, stats.documentCount)
        assertEquals(0, stats.chunkCount)
    }
}
