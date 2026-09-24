package com.ragkit.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ragkit.core.RagKit
import com.ragkit.core.model.RagDocument
import com.ragkit.core.model.RagSearchResult
import com.ragkit.core.model.RagStats
import com.ragkit.embedding.mediapipe.MediaPipeEmbeddingEngine
import com.ragkit.sample.data.SampleData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ModelStatus {
    object Idle : ModelStatus
    data class Downloading(val progress: Float) : ModelStatus
    object Ready : ModelStatus
    data class Error(val message: String) : ModelStatus
}

data class RagUiState(
    val modelStatus: ModelStatus = ModelStatus.Idle,
    val documents: List<RagDocument> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<RagSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val stats: RagStats? = null,
    val infoMessage: String? = null
)

class RagViewModel(
    private val ragKit: RagKit,
    private val embeddingEngine: MediaPipeEmbeddingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(RagUiState())
    val uiState: StateFlow<RagUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeModelDownload()
        initialize()
    }

    private fun observeModelDownload() {
        viewModelScope.launch {
            embeddingEngine.downloadProgress.collect { progress ->
                if (progress < 1.0f && progress > 0.0f) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Downloading(progress)) }
                } else if (progress >= 1.0f && embeddingEngine.isReady()) {
                    _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                }
            }
        }
    }

    fun initialize() {
        viewModelScope.launch {
            _uiState.update { it.copy(modelStatus = ModelStatus.Downloading(0.0f)) }
            val result = ragKit.initialize()
            if (result.isSuccess) {
                _uiState.update { it.copy(modelStatus = ModelStatus.Ready) }
                refreshStats()
                // If storage is empty, populate initial sample notes
                val currentStats = ragKit.getStats().getOrNull()
                if (currentStats != null && currentStats.documentCount == 0) {
                    loadSampleNotes()
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Model yüklenemedi."
                _uiState.update { it.copy(modelStatus = ModelStatus.Error(errorMsg)) }
            }
        }
    }

    fun addNote(text: String, category: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val doc = RagDocument(
                text = text.trim(),
                source = category.trim().lowercase(),
                metadata = mapOf("category" to category.trim())
            )
            val result = ragKit.index(doc)
            if (result.isSuccess) {
                _uiState.update { current ->
                    current.copy(
                        documents = listOf(doc) + current.documents,
                        infoMessage = "Not başarıyla indekslendi."
                    )
                }
                refreshStats()
                if (_uiState.value.searchQuery.isNotBlank()) {
                    onSearchQueryChanged(_uiState.value.searchQuery)
                }
            } else {
                _uiState.update { it.copy(infoMessage = "Not eklenirken hata: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun deleteNote(documentId: String) {
        viewModelScope.launch {
            val result = ragKit.deleteById(documentId)
            if (result.isSuccess) {
                _uiState.update { current ->
                    current.copy(
                        documents = current.documents.filter { it.id != documentId },
                        infoMessage = "Not silindi."
                    )
                }
                refreshStats()
                if (_uiState.value.searchQuery.isNotBlank()) {
                    onSearchQueryChanged(_uiState.value.searchQuery)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val result = ragKit.search(query, limit = 10)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        searchResults = result.getOrDefault(emptyList()),
                        isSearching = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false
                    )
                }
            }
        }
    }

    fun loadSampleNotes() {
        viewModelScope.launch {
            val sampleDocs = SampleData.initialNotes
            val result = ragKit.index(sampleDocs)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        documents = sampleDocs,
                        infoMessage = "${sampleDocs.size} adet örnek not eklendi."
                    )
                }
                refreshStats()
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            ragKit.clear()
            _uiState.update {
                it.copy(
                    documents = emptyList(),
                    searchResults = emptyList(),
                    infoMessage = "Tüm veritabanı temizlendi."
                )
            }
            refreshStats()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val stats = ragKit.getStats().getOrNull()
            _uiState.update { it.copy(stats = stats) }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
