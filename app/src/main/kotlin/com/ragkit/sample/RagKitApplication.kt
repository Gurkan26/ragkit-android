package com.ragkit.sample

import android.app.Application
import com.ragkit.core.RagKit
import com.ragkit.embedding.mediapipe.MediaPipeEmbeddingEngine
import com.ragkit.storage.room.RoomRagStorage

/**
 * Application class providing a centralized [RagKit] singleton instance.
 */
class RagKitApplication : Application() {

    lateinit var embeddingEngine: MediaPipeEmbeddingEngine
        private set

    lateinit var ragKit: RagKit
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        embeddingEngine = MediaPipeEmbeddingEngine(this)
        val storage = RoomRagStorage(this)

        ragKit = RagKit.builder(this)
            .embeddingEngine(embeddingEngine)
            .storage(storage)
            .config {
                maxChunkSize = 500
                chunkOverlap = 50
                maxResults = 10
                minScore = 0.25f
            }
            .build()
    }

    companion object {
        lateinit var instance: RagKitApplication
            private set
    }
}
