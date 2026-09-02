package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FileManager
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditorUiState(
    val isLoading: Boolean = true,
    val project: ProjectEntity? = null,
    val files: List<File> = emptyList(),
    val currentFile: File? = null,
    val fileContent: String = "",
    val isPreviewing: Boolean = false,
    val fontSize: Float = 14f,
    val previewWidth: Int? = null,
    val previewHeight: Int? = null,
    val previewZoom: Float = 1f
)

class EditorViewModel(
    application: Application,
    private val repository: ProjectRepository,
    private val projectId: Int
) : AndroidViewModel(application) {

    private val fileManager = FileManager(application)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    private var saveJob: Job? = null

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId)
            if (project != null) {
                // Migrate old DB data to files if needed
                fileManager.createDefaultFilesIfNeeded(projectId, project.htmlContent, project.cssContent, project.jsContent)
                
                val files = fileManager.listFiles(projectId)
                val currentFile = files.find { it.name == "index.html" } ?: files.firstOrNull()
                val content = currentFile?.let { fileManager.readFile(it) } ?: ""

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        project = project,
                        files = files,
                        currentFile = currentFile,
                        fileContent = content
                    ) 
                }
            }
        }
    }

    fun openFile(file: File) {
        // Save current first
        saveCurrentFileNow()
        val content = fileManager.readFile(file)
        _uiState.update { it.copy(currentFile = file, fileContent = content) }
    }

    fun updateFileContent(content: String) {
        _uiState.update { it.copy(fileContent = content) }
        debounceSave()
    }

    private fun debounceSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000)
            saveCurrentFileNow()
        }
    }

    fun saveCurrentFileNow() {
        val state = _uiState.value
        val file = state.currentFile ?: return
        fileManager.writeFile(projectId, file.name, state.fileContent)
        
        // Update project timestamp
        viewModelScope.launch {
            state.project?.let {
                repository.updateProject(it.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun createFile(fileName: String) {
        fileManager.writeFile(projectId, fileName, "")
        val files = fileManager.listFiles(projectId)
        val newFile = files.find { it.name == fileName }
        if (newFile != null) {
            _uiState.update { it.copy(files = files) }
            openFile(newFile)
        }
    }

    fun renameFile(oldFile: File, newName: String) {
        fileManager.renameFile(projectId, oldFile.name, newName)
        val files = fileManager.listFiles(projectId)
        val newFile = files.find { it.name == newName }
        _uiState.update { 
            it.copy(
                files = files,
                currentFile = if (it.currentFile?.name == oldFile.name) newFile else it.currentFile
            )
        }
    }

    fun importFileWithOriginalName(uri: android.net.Uri) {
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        var fileName = "imported_file"
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        fileManager.importFileFromUri(projectId, uri, fileName)
        val files = fileManager.listFiles(projectId)
        _uiState.update { it.copy(files = files) }
    }

    fun importImage(uri: android.net.Uri, fileName: String) {
        fileManager.importFileFromUri(projectId, uri, fileName)
        val files = fileManager.listFiles(projectId)
        _uiState.update { it.copy(files = files) }
    }

    fun deleteFile(file: File) {
        fileManager.deleteFile(projectId, file.name)
        val files = fileManager.listFiles(projectId)
        val nextFile = files.firstOrNull()
        val content = nextFile?.let { fileManager.readFile(it) } ?: ""
        
        _uiState.update { 
            it.copy(
                files = files, 
                currentFile = nextFile,
                fileContent = content
            ) 
        }
    }

    fun togglePreview() {
        // Save before preview
        saveCurrentFileNow()
        _uiState.update { it.copy(isPreviewing = !it.isPreviewing) }
    }
    
    fun setPreviewSize(width: Int?, height: Int?) {
        _uiState.update { it.copy(previewWidth = width, previewHeight = height) }
    }
    
    fun setPreviewZoom(zoom: Float) {
        _uiState.update { it.copy(previewZoom = zoom) }
    }

    fun changeFontSize(delta: Float) {
        _uiState.update { state ->
            val newSize = (state.fontSize + delta).coerceIn(10f, 32f)
            state.copy(fontSize = newSize)
        }
    }
}

class EditorViewModelFactory(
    private val application: Application,
    private val repository: ProjectRepository,
    private val projectId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(application, repository, projectId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
