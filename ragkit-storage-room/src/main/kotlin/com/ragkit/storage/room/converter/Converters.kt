package com.ragkit.storage.room.converter

import androidx.room.TypeConverter
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Room type converters for serializing vector embeddings (FloatArray) and key-value metadata maps.
 */
class Converters {

    /**
     * Converts a [FloatArray] into a compact binary [ByteArray] (4 bytes per float, Little Endian).
     */
    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in array) {
            buffer.putFloat(f)
        }
        return buffer.array()
    }

    /**
     * Deserializes a binary [ByteArray] back into a [FloatArray].
     */
    @TypeConverter
    fun toFloatArray(bytes: ByteArray?): FloatArray? {
        if (bytes == null || bytes.isEmpty()) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatCount = bytes.size / 4
        val result = FloatArray(floatCount)
        for (i in 0 until floatCount) {
            result[i] = buffer.float
        }
        return result
    }

    /**
     * Serializes a [Map] of string key-values into a JSON string.
     */
    @TypeConverter
    fun fromMetadataMap(map: Map<String, String>?): String {
        if (map == null || map.isEmpty()) return "{}"
        val json = JSONObject()
        for ((key, value) in map) {
            json.put(key, value)
        }
        return json.toString()
    }

    /**
     * Deserializes a JSON string into a [Map] of string key-values.
     */
    @TypeConverter
    fun toMetadataMap(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optString(key, "")
            }
            map
        }.getOrDefault(emptyMap())
    }
}
