package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectRepository
import com.example.ui.components.CodeEditor
import com.example.ui.components.PreviewWebView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Int,
    repository: ProjectRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory(repository, projectId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var consoleLogs by remember { mutableStateOf(listOf<String>()) }
    var showConsole by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val project = uiState.project ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveProject(); Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { viewModel.togglePreview() }) {
                        Icon(if (uiState.isPreviewing) Icons.Default.Code else Icons.Default.PlayArrow, contentDescription = "Toggle Preview")
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isPreviewing) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            val code = when (uiState.currentTab) {
                                EditorTab.HTML -> project.htmlContent
                                EditorTab.CSS -> project.cssContent
                                EditorTab.JS -> project.jsContent
                            }
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { viewModel.clearCurrentFile() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
                        }
                        IconButton(onClick = { viewModel.changeFontSize(2f) }) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }
                        IconButton(onClick = { viewModel.changeFontSize(-2f) }) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }
                    }
                }
            } else {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = { showConsole = true }) {
                            Icon(Icons.Default.Terminal, contentDescription = "Console")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Console (${consoleLogs.size})")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.isPreviewing) {
                TabRow(selectedTabIndex = uiState.currentTab.ordinal) {
                    Tab(
                        selected = uiState.currentTab == EditorTab.HTML,
                        onClick = { viewModel.setTab(EditorTab.HTML) },
                        text = { Text("index.html") }
                    )
                    Tab(
                        selected = uiState.currentTab == EditorTab.CSS,
                        onClick = { viewModel.setTab(EditorTab.CSS) },
                        text = { Text("style.css") }
                    )
                    Tab(
                        selected = uiState.currentTab == EditorTab.JS,
                        onClick = { viewModel.setTab(EditorTab.JS) },
                        text = { Text("script.js") }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.currentTab) {
                        EditorTab.HTML -> CodeEditor(
                            code = project.htmlContent,
                            onCodeChange = viewModel::updateHtml,
                            language = "html",
                            fontSize = uiState.fontSize
                        )
                        EditorTab.CSS -> CodeEditor(
                            code = project.cssContent,
                            onCodeChange = viewModel::updateCss,
                            language = "css",
                            fontSize = uiState.fontSize
                        )
                        EditorTab.JS -> CodeEditor(
                            code = project.jsContent,
                            onCodeChange = viewModel::updateJs,
                            language = "js",
                            fontSize = uiState.fontSize
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    PreviewWebView(
                        htmlContent = project.htmlContent,
                        cssContent = project.cssContent,
                        jsContent = project.jsContent,
                        onConsoleMessage = { msg ->
                            consoleLogs = consoleLogs + msg
                        }
                    )
                }
            }
        }
    }

    if (showConsole) {
        AlertDialog(
            onDismissRequest = { showConsole = false },
            title = { Text("Console Logs") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (consoleLogs.isEmpty()) {
                        Text("No logs.")
                    } else {
                        consoleLogs.forEach { log ->
                            Text(log, style = MaterialTheme.typography.bodySmall)
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConsole = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { consoleLogs = emptyList() }) { Text("Clear") }
            }
        )
    }
}
