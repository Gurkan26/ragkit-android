package com.ragkit.core.engine

/**
 * Interface defining the contract for converting natural language text into dense numerical vector embeddings.
 *
 * Implementations (such as MediaPipe Text Embedder) are responsible for model lifecycle,
 * batch processing, and vector inference.
 */
interface EmbeddingEngine {

    /**
     * Initializes the underlying embedding model, downloading assets or allocating native resources if necessary.
     *
     * @return [Result.success] if initialized properly, or [Result.failure] if model initialization failed.
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Generates a normalized vector embedding for a single text string.
     *
     * @param text The input text to embed. Must not be empty.
     * @return [Result.success] containing the float array vector embedding, or [Result.failure] on error.
     */
    suspend fun embed(text: String): Result<FloatArray>

    /**
     * Generates normalized vector embeddings for a batch of text strings.
     *
     * @param texts The list of input texts to embed.
     * @return [Result.success] containing a list of vector embeddings corresponding to the input order, or [Result.failure].
     */
    suspend fun embedBatch(texts: List<String>): Result<List<FloatArray>>

    /**
     * Checks if the embedding engine is loaded, warm, and ready for inference.
     *
     * @return `true` if ready, `false` otherwise.
     */
    fun isReady(): Boolean

    /**
     * Returns the name, version, or filename of the active embedding model.
     *
     * @return String representation of the model name.
     */
    fun getModelName(): String

    /**
     * Returns the approximate memory or disk size of the model in bytes.
     *
     * @return Model size in bytes.
     */
    fun getModelSizeBytes(): Long

    /**
     * Releases any native resources, thread pools, or loaded model memory.
     */
    fun release()
}
