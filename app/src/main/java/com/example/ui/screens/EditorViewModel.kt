package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val isLoading: Boolean = true,
    val project: ProjectEntity? = null,
    val currentTab: EditorTab = EditorTab.HTML,
    val isPreviewing: Boolean = false,
    val fontSize: Float = 14f,
    val previewWidth: Int? = null, // null means fullscreen/match_parent
    val previewHeight: Int? = null,
    val previewZoom: Float = 1f
)

enum class EditorTab {
    HTML, CSS, JS
}

class EditorViewModel(
    private val repository: ProjectRepository,
    private val projectId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private var saveJob: Job? = null

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            _uiState.update { it.copy(isLoading = false, project = project) }
        }
    }

    fun setTab(tab: EditorTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun togglePreview() {
        _uiState.update { it.copy(isPreviewing = !it.isPreviewing) }
    }
    
    fun setPreviewSize(width: Int?, height: Int?) {
        _uiState.update { it.copy(previewWidth = width, previewHeight = height) }
    }
    
    fun setPreviewZoom(zoom: Float) {
        _uiState.update { it.copy(previewZoom = zoom) }
    }

    fun updateHtml(html: String) {
        _uiState.update { state ->
            state.copy(project = state.project?.copy(htmlContent = html, updatedAt = System.currentTimeMillis()))
        }
        debounceSave()
    }

    fun updateCss(css: String) {
        _uiState.update { state ->
            state.copy(project = state.project?.copy(cssContent = css, updatedAt = System.currentTimeMillis()))
        }
        debounceSave()
    }

    fun updateJs(js: String) {
        _uiState.update { state ->
            state.copy(project = state.project?.copy(jsContent = js, updatedAt = System.currentTimeMillis()))
        }
        debounceSave()
    }

    private fun debounceSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000) // 1 second debounce
            saveProjectNow()
        }
    }

    fun saveProjectNow() {
        viewModelScope.launch {
            _uiState.value.project?.let {
                repository.updateProject(it)
            }
        }
    }

    fun clearCurrentFile() {
        _uiState.update { state ->
            val project = state.project ?: return@update state
            val updatedProject = when (state.currentTab) {
                EditorTab.HTML -> project.copy(htmlContent = "")
                EditorTab.CSS -> project.copy(cssContent = "")
                EditorTab.JS -> project.copy(jsContent = "")
            }
            state.copy(project = updatedProject)
        }
        debounceSave()
    }

    fun changeFontSize(delta: Float) {
        _uiState.update { state ->
            val newSize = (state.fontSize + delta).coerceIn(10f, 32f)
            state.copy(fontSize = newSize)
        }
    }
}

class EditorViewModelFactory(
    private val repository: ProjectRepository,
    private val projectId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(repository, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
