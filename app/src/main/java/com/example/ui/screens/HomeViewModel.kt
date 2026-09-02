package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FileManager
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val repository: ProjectRepository
) : AndroidViewModel(application) {

    private val fileManager = FileManager(application)

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createProject(name: String, template: String, onProjectCreated: (Int) -> Unit) {
        viewModelScope.launch {
            val newProject = ProjectEntity(
                name = name,
                htmlContent = "",
                cssContent = "",
                jsContent = ""
            )
            val id = repository.insertProject(newProject).toInt()
            
            // Apply the chosen template immediately
            fileManager.applyTemplate(id, template)
            
            onProjectCreated(id)
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val duplicate = project.copy(
                id = 0,
                name = "${project.name} (Copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertProject(duplicate)
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch {
            repository.updateProject(project.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }
}

class HomeViewModelFactory(
    private val application: Application,
    private val repository: ProjectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
