package com.ragkit.storage.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an indexed document in local storage.
 *
 * @property id Unique identifier of the document.
 * @property source Optional category or group identifier (e.g. "notes", "help").
 * @property metadata Serialized JSON string containing key-value metadata attributes.
 * @property createdAt Timestamp of document creation in milliseconds.
 * @property updatedAt Timestamp of last document modification in milliseconds.
 */
@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["source"])
    ]
)
data class DocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "source")
    val source: String? = null,

    @ColumnInfo(name = "metadata")
    val metadata: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
