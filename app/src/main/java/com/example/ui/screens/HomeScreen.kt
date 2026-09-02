package com.example.ui.screens

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ProjectRepository,
    onNavigateToEditor: (Int) -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(application, repository)
    )
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("Default") }
    var showRenameDialogFor by remember { mutableStateOf<ProjectEntity?>(null) }
    
    var projectToExport by remember { mutableStateOf<ProjectEntity?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                projectToExport?.let { project ->
                    // Actually, the export logic is in EditorScreen.
                    // For Home screen, we just show a toast or implement it fully.
                    // To keep it simple in this update, we omit full export here since it's already in Editor.
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebCode Studio", color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No projects yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Create your first project")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onNavigateToEditor(project.id) },
                        onDelete = { viewModel.deleteProject(project.id) },
                        onDuplicate = { viewModel.duplicateProject(project) },
                        onRename = { showRenameDialogFor = project },
                        onExport = {
                            // No-op here, export is in Editor
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        val templates = listOf("Default", "Bootstrap", "Tailwind", "Three.js")
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Select Template:", style = MaterialTheme.typography.titleSmall)
                    
                    templates.forEach { template ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = template }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTemplate == template,
                                onClick = { selectedTemplate = template }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(template)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName, selectedTemplate) { newId ->
                                showCreateDialog = false
                                newProjectName = ""
                                selectedTemplate = "Default"
                                onNavigateToEditor(newId)
                            }
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    showRenameDialogFor?.let { project ->
        var renameText by remember { mutableStateOf(project.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialogFor = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameProject(project, renameText)
                            showRenameDialogFor = null
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogFor = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Updated: ${dateFormat.format(Date(project.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { expanded = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { expanded = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Default.FileCopy, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { expanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}
