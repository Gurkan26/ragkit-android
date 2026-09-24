package com.ragkit.storage.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an embedded chunk belonging to a [DocumentEntity].
 *
 * @property id Unique identifier of the chunk.
 * @property documentId The ID of the parent document.
 * @property text The raw text slice of the chunk.
 * @property embedding Serialized dense float vector stored as a binary blob.
 * @property chunkIndex Positional order of this chunk within the document.
 * @property metadata Serialized JSON string containing key-value metadata attributes.
 * @property createdAt Timestamp when this chunk was created.
 */
@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["document_id"]),
        Index(value = ["chunk_index"])
    ]
)
data class ChunkEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,

    @ColumnInfo(name = "chunk_index")
    val chunkIndex: Int,

    @ColumnInfo(name = "metadata")
    val metadata: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkEntity
        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (chunkIndex != other.chunkIndex) return false
        if (metadata != other.metadata) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + metadata.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
