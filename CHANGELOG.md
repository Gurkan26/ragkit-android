# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-09-24

### Added
- **ragkit-core**: Initial release with core domain interfaces (`EmbeddingEngine`, `RagStorage`, `TextChunker`).
- **ragkit-core**: Main `RagKit` orchestrator class with Fluent Builder and Kotlin DSL configuration.
- **ragkit-core**: Hierarchical `SimpleTextChunker` dividing text by paragraph, sentence, and overlapping character boundaries.
- **ragkit-core**: Sealed interface `RagException` for domain-specific error handling.
- **ragkit-storage-room**: Room SQLite vector persistence with binary BLOB embedding serialization and brute-force normalized cosine similarity.
- **ragkit-embedding-mediapipe**: Google MediaPipe Text Embedder integration with download-on-first-use, checksum verification, and `ComponentCallbacks2` memory trimming.
- **app**: Material 3 Jetpack Compose sample application demonstrating real-time semantic search, note management, and model telemetry.
- **ci**: GitHub Actions CI workflow for automated testing, linting, and build verification.
