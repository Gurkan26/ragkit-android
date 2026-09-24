package com.ragkit.embedding.mediapipe

/**
 * Configuration detailing model source, download location, and verification criteria for embedding models.
 *
 * @property modelName The identifier or filename of the model (e.g., "universal_sentence_encoder.tflite").
 * @property modelUrl The remote HTTPS URL to download the model from if not present locally.
 * @property modelSizeBytes The expected file size in bytes for download validation and progress calculation.
 * @property embeddingDimension Dimension count of the output vector (e.g., 100 for Universal Sentence Encoder).
 * @property checksum Optional expected SHA-256 checksum string for model integrity verification.
 * @property assetFilePath Optional bundled asset file path (e.g., "models/use.tflite") if bundled in assets.
 */
data class ModelConfig(
    val modelName: String = DEFAULT_MODEL_NAME,
    val modelUrl: String = DEFAULT_MODEL_URL,
    val modelSizeBytes: Long = DEFAULT_MODEL_SIZE_BYTES,
    val embeddingDimension: Int = DEFAULT_EMBEDDING_DIMENSION,
    val checksum: String? = null,
    val assetFilePath: String? = null
) {
    init {
        require(modelName.isNotBlank()) { "modelName cannot be blank." }
        require(embeddingDimension > 0) { "embeddingDimension must be positive." }
        require(modelSizeBytes >= 0L) { "modelSizeBytes cannot be negative." }
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "universal_sentence_encoder.tflite"
        const val DEFAULT_MODEL_URL =
            "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
        const val DEFAULT_MODEL_SIZE_BYTES = 998_000L
        const val DEFAULT_EMBEDDING_DIMENSION = 100

        /**
         * Default configuration pointing to the Google MediaPipe Universal Sentence Encoder model.
         */
        val DEFAULT = ModelConfig()
    }
}
