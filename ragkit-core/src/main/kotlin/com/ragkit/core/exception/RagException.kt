package com.ragkit.core.exception

/**
 * Sealed interface representing all domain-specific errors and exceptions produced by RagKit.
 */
sealed interface RagException {
    val message: String
    val cause: Throwable?

    /**
     * Thrown when the embedding model is queried before being properly initialized or loaded.
     */
    class ModelNotReadyException(
        override val message: String = "Embedding model is not ready. Call initialize() first.",
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when a remote embedding model download fails due to network or verification errors.
     */
    class ModelDownloadException(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when the embedding engine fails to vectorize the provided text.
     */
    class EmbeddingFailedException(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when a database or disk persistence operation fails in the storage engine.
     */
    class StorageException(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when a document exceeds maximum allowed length constraints.
     */
    class DocumentTooLongException(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when a search query is empty or whitespace-only.
     */
    class QueryEmptyException(
        override val message: String = "Search query cannot be empty or blank.",
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException

    /**
     * Thrown when initializing RagKit or its subsystems encounters a fatal error.
     */
    class InitializationException(
        override val message: String,
        override val cause: Throwable? = null
    ) : Exception(message, cause), RagException
}
