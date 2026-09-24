package com.ragkit.storage.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ragkit.storage.room.converter.Converters
import com.ragkit.storage.room.dao.RagDao
import com.ragkit.storage.room.entity.ChunkEntity
import com.ragkit.storage.room.entity.DocumentEntity

/**
 * Room database instance storing documents, text chunks, and embedding vectors.
 */
@Database(
    entities = [
        DocumentEntity::class,
        ChunkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RagDatabase : RoomDatabase() {

    abstract fun ragDao(): RagDao

    companion object {
        const val DATABASE_NAME = "ragkit_database.db"

        @Volatile
        private var INSTANCE: RagDatabase? = null

        fun getInstance(context: Context): RagDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RagDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
