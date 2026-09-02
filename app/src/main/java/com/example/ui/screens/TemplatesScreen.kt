package com.example.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectRepository

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    repository: ProjectRepository,
    onNavigateToEditor: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(application, repository)
    )

    var showNameDialogForTemplate by remember { mutableStateOf<String?>(null) }
    var newProjectName by remember { mutableStateOf("") }

    val templates = listOf(
        ProjectTemplate("Default", "Blank Project", "Start from scratch with basic HTML/CSS/JS", Icons.Default.InsertDriveFile, MaterialTheme.colorScheme.primary),
        ProjectTemplate("TicTacToe", "Tic-Tac-Toe", "Classic XO game with HTML, CSS, and JS logic", Icons.Default.Gamepad, MaterialTheme.colorScheme.tertiary),
        ProjectTemplate("Calculator", "Calculator", "A functional web-based calculator", Icons.Default.Calculate, MaterialTheme.colorScheme.secondary),
        ProjectTemplate("LandingPage", "Landing Page", "Modern responsive landing page template", Icons.Default.Web, MaterialTheme.colorScheme.primary),
        ProjectTemplate("TodoList", "To-Do List", "Task tracker with local JS state", Icons.Default.Checklist, MaterialTheme.colorScheme.secondary),
        ProjectTemplate("Bootstrap", "Bootstrap 5", "Pre-configured Bootstrap 5 setup", Icons.Default.Brush, MaterialTheme.colorScheme.tertiary),
        ProjectTemplate("Tailwind", "Tailwind CSS", "Pre-configured Tailwind CSS setup", Icons.Default.Style, MaterialTheme.colorScheme.primary),
        ProjectTemplate("Three.js", "Three.js 3D", "Basic 3D cube rendering setup", Icons.Default.ViewInAr, MaterialTheme.colorScheme.secondary)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates Gallery") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { showNameDialogForTemplate = template.id },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            template.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = template.color
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    showNameDialogForTemplate?.let { templateId ->
        AlertDialog(
            onDismissRequest = { showNameDialogForTemplate = null },
            title = { Text("Project Name") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Enter project name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.createProject(newProjectName, templateId) { newId ->
                                showNameDialogForTemplate = null
                                newProjectName = ""
                                onNavigateToEditor(newId)
                            }
                        }
                    }
                ) { Text("Create & Open") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialogForTemplate = null }) { Text("Cancel") }
            }
        )
    }
}
