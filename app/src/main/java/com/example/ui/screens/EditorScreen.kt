package com.example.ui.screens

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectRepository
import com.example.ui.components.CodeEditor
import com.example.ui.components.PreviewWebView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleLog(val level: String, val message: String, val timestamp: Long)

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var consoleLogs by remember { mutableStateOf(listOf<ConsoleLog>()) }
    var showConsole by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val project = uiState.project ?: return

    Scaffold(
        modifier = Modifier.onKeyEvent { keyEvent ->
            if (keyEvent.isCtrlPressed && keyEvent.key == Key.S && keyEvent.type == KeyEventType.KeyUp) {
                viewModel.saveProjectNow()
                Toast.makeText(context, "Saved (Ctrl+S)", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "WebCode Studio", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.saveProjectNow()
                        Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show() 
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    if (!isLandscape) {
                        IconButton(onClick = { viewModel.togglePreview() }) {
                            Icon(if (uiState.isPreviewing) Icons.Default.Code else Icons.Default.PlayArrow, contentDescription = "Toggle Preview")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isPreviewing || isLandscape) {
                BottomAppBar(modifier = Modifier.height(56.dp)) {
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
                            Toast.makeText(context, "Copied exactly", Toast.LENGTH_SHORT).show()
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
                BottomAppBar(modifier = Modifier.height(56.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.setPreviewSize(390, 844) }) {
                                Icon(Icons.Default.Phone, contentDescription = "Phone")
                            }
                            IconButton(onClick = { viewModel.setPreviewSize(768, 1024) }) {
                                Icon(Icons.Default.Tablet, contentDescription = "Tablet")
                            }
                            IconButton(onClick = { viewModel.setPreviewSize(null, null) }) {
                                Icon(Icons.Default.DesktopMac, contentDescription = "Desktop/Full")
                            }
                        }
                        Button(onClick = { showConsole = true }, modifier = Modifier.padding(end = 16.dp)) {
                            Icon(Icons.Default.Terminal, contentDescription = "Console")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Console (${consoleLogs.size})")
                        }
                    }
                }
            }
        }
    ) { padding ->
        val editorContent = @Composable {
            Column(modifier = Modifier.fillMaxSize()) {
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
            }
        }

        val previewContent = @Composable {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLandscape) {
                    // Toolbar for preview in landscape
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = { viewModel.setPreviewSize(390, 844) }) {
                                Icon(Icons.Default.Phone, contentDescription = "Phone")
                            }
                            IconButton(onClick = { viewModel.setPreviewSize(768, 1024) }) {
                                Icon(Icons.Default.Tablet, contentDescription = "Tablet")
                            }
                            IconButton(onClick = { viewModel.setPreviewSize(null, null) }) {
                                Icon(Icons.Default.DesktopMac, contentDescription = "Desktop/Full")
                            }
                        }
                        IconButton(onClick = { showConsole = true }) {
                            Icon(Icons.Default.Terminal, contentDescription = "Console", tint = if (consoleLogs.isNotEmpty()) MaterialTheme.colorScheme.error else LocalContentColor.current)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background)) {
                    PreviewWebView(
                        htmlContent = project.htmlContent,
                        cssContent = project.cssContent,
                        jsContent = project.jsContent,
                        previewWidth = uiState.previewWidth,
                        previewHeight = uiState.previewHeight,
                        previewZoom = uiState.previewZoom,
                        onConsoleMessage = { level, msg ->
                            consoleLogs = consoleLogs + ConsoleLog(level, msg, System.currentTimeMillis())
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        editorContent()
                    }
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        previewContent()
                    }
                }
            } else {
                if (!uiState.isPreviewing) {
                    editorContent()
                } else {
                    previewContent()
                }
            }
        }
    }

    if (showConsole) {
        ConsoleDialog(
            logs = consoleLogs,
            onDismiss = { showConsole = false },
            onClear = { consoleLogs = emptyList() },
            onCopyAll = {
                val allText = consoleLogs.joinToString("\n") { "[${it.level}] ${it.message}" }
                clipboardManager.setText(AnnotatedString(allText))
                Toast.makeText(context, "Copied all logs", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleDialog(
    logs: List<ConsoleLog>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onCopyAll: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Console", style = MaterialTheme.typography.titleLarge)
                Row {
                    IconButton(onClick = onCopyAll) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All")
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
            
            Divider()
            
            // Logs
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(logs) { log ->
                        val color = when (log.level) {
                            "ERROR" -> Color(0xFFCF6679) // Red
                            "WARNING" -> Color(0xFFE6C74D) // Yellow
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = log.level,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = color
                            )
                            Divider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
