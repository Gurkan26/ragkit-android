package com.ragkit.embedding.mediapipe

import android.content.Context
import com.ragkit.core.exception.RagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Handles caching, downloading, and checksum verification of on-device machine learning models.
 *
 * @param context Android [Context] used to resolve internal app storage directories.
 */
class ModelDownloader(private val context: Context) {

    private val modelsDirectory: File by lazy {
        File(context.filesDir, "ragkit_models").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Resolves the model file, downloading it from [ModelConfig.modelUrl] if it is not already cached
     * and valid.
     *
     * Supports cooperative coroutine cancellation and emits download progress `[0.0f, 1.0f]`.
     *
     * @param config Configuration specifying model name, URL, and optional checksum.
     * @param onProgress Callback invoked on the caller's coroutine dispatcher with normalized progress.
     * @return [Result.success] containing the verified [File], or [Result.failure] on error.
     */
    suspend fun downloadModel(
        config: ModelConfig,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val destinationFile = File(modelsDirectory, config.modelName)

            // Cache check: if destination file already exists and passes checksum / non-empty check
            if (destinationFile.exists() && destinationFile.length() > 0) {
                if (config.checksum == null || verifyChecksum(destinationFile, config.checksum)) {
                    onProgress(1.0f)
                    return@runCatching destinationFile
                } else {
                    // Stale or corrupted file
                    destinationFile.delete()
                }
            }

            val tempFile = File(modelsDirectory, "${config.modelName}.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val url = URL(config.modelUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw RagException.ModelDownloadException(
                        "Server returned HTTP response code: $responseCode from ${config.modelUrl}"
                    )
                }

                val totalBytes = if (connection.contentLengthLong > 0) {
                    connection.contentLengthLong
                } else {
                    config.modelSizeBytes
                }

                var bytesReadTotal = 0L
                val buffer = ByteArray(BUFFER_SIZE)

                connection.inputStream.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            outputStream.write(buffer, 0, bytesRead)
                            bytesReadTotal += bytesRead

                            if (totalBytes > 0) {
                                val progress = (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0.0f, 1.0f)
                                onProgress(progress)
                            }
                        }
                        outputStream.flush()
                    }
                }

                // Checksum verification
                if (config.checksum != null && !verifyChecksum(tempFile, config.checksum)) {
                    tempFile.delete()
                    throw RagException.ModelDownloadException(
                        "Model integrity check failed: checksum mismatch for ${config.modelName}"
                    )
                }

                if (tempFile.renameTo(destinationFile)) {
                    onProgress(1.0f)
                    destinationFile
                } else {
                    throw RagException.ModelDownloadException("Failed to finalize downloaded model file.")
                }
            } finally {
                connection.disconnect()
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }.recoverCatching { error ->
            throw if (error is RagException) error else RagException.ModelDownloadException(
                "Model download failed: ${error.message}",
                error
            )
        }
    }

    /**
     * Checks if the model already exists in local storage and is non-empty.
     */
    fun isModelCached(config: ModelConfig): Boolean {
        val file = File(modelsDirectory, config.modelName)
        return file.exists() && file.length() > 0
    }

    /**
     * Retrieves the cached model file if it exists, or `null`.
     */
    fun getCachedModelFile(config: ModelConfig): File? {
        val file = File(modelsDirectory, config.modelName)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Verifies SHA-256 checksum of a file.
     */
    private fun verifyChecksum(file: File, expectedHex: String): Boolean {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculatedHex = digest.digest().joinToString("") { "%02x".format(it) }
            calculatedHex.equals(expectedHex.trim(), ignoreCase = true)
        }.getOrDefault(false)
    }

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}
