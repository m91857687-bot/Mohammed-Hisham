package com.example.ui.screens

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProjectRepository
import com.example.data.ZipUtil
import com.example.ui.components.CodeEditor
import com.example.ui.components.PreviewWebView
import kotlinx.coroutines.launch
import java.io.File
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
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModelFactory(application, repository, projectId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var consoleLogs by remember { mutableStateOf(listOf<ConsoleLog>()) }
    var showConsole by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameImageDialog by remember { mutableStateOf<Uri?>(null) }
    var renameImageName by remember { mutableStateOf("image.png") }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showRenameFileDialogFor by remember { mutableStateOf<File?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFileWithOriginalName(uri)
            Toast.makeText(context, "File Imported", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            showRenameImageDialog = uri
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val project = uiState.project ?: return
    BackHandler {
        if (uiState.isPreviewing) {
            viewModel.togglePreview()
        } else {
            showExitConfirmation = true
        }
    }

    val exportProject = {
        viewModel.saveCurrentFileNow()
        val projectDir = File(context.filesDir, "projects/project_$projectId")
        val zipFile = File(context.cacheDir, "${project.name}.zip")
        try {
            ZipUtil.zipDirectory(projectDir, zipFile)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Project"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !uiState.isPreviewing,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Project Files", 
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Files", style = MaterialTheme.typography.titleSmall)
                    Row {
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Import File", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = "Import Image", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showNewFileDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New File", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.files) { file ->
                        val isSelected = uiState.currentFile?.name == file.name
                        val isImage = file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".gif") || file.name.endsWith(".svg")
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (!isImage) {
                                        viewModel.openFile(file)
                                    } else {
                                        Toast.makeText(context, "Cannot edit images directly", Toast.LENGTH_SHORT).show()
                                    }
                                    if (!isLandscape) {
                                        scope.launch { drawerState.close() }
                                    }
                                }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when {
                                        file.name.endsWith(".html") -> Icons.Default.Html
                                        file.name.endsWith(".css") -> Icons.Default.Style
                                        file.name.endsWith(".js") -> Icons.Default.Javascript
                                        isImage -> Icons.Default.Image
                                        else -> Icons.Default.InsertDriveFile
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    file.name, 
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                IconButton(onClick = { showRenameFileDialogFor = file }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (uiState.files.size > 1) {
                                    IconButton(onClick = { viewModel.deleteFile(file) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.onKeyEvent { keyEvent ->
                if (keyEvent.isCtrlPressed && keyEvent.key == Key.S && keyEvent.type == KeyEventType.KeyUp) {
                    viewModel.saveCurrentFileNow()
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
                                text = uiState.currentFile?.name ?: "No file open", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        TextButton(onClick = { showExitConfirmation = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Project", tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(4.dp))
                            Text("Exit", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Folder, contentDescription = "Files")
                        }
                        IconButton(onClick = exportProject) {
                            Icon(Icons.Default.Download, contentDescription = "Export Zip")
                        }
                        IconButton(onClick = { 
                             viewModel.saveCurrentFileNow()
                             Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show() 
                         }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                        if (!isLandscape) {
                            if (uiState.isPreviewing) {
                                Button(
                                    onClick = { viewModel.togglePreview() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Exit Preview")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { viewModel.togglePreview() },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Run")
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!uiState.isPreviewing || isLandscape) {
                    BottomAppBar(
                        actions = {
                            IconButton(onClick = { viewModel.changeFontSize(-2f) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                            }
                            IconButton(onClick = { viewModel.changeFontSize(2f) }) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            val editorContent = @Composable {
                Column(modifier = Modifier.fillMaxSize()) {
                    val language = when {
                        uiState.currentFile?.name?.endsWith(".html") == true -> "html"
                        uiState.currentFile?.name?.endsWith(".css") == true -> "css"
                        uiState.currentFile?.name?.endsWith(".js") == true -> "js"
                        else -> "txt"
                    }
                    
                    CodeEditor(
                        code = uiState.fileContent,
                        onCodeChange = viewModel::updateFileContent,
                        language = language,
                        fontSize = uiState.fontSize,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val previewContent = @Composable {
                val currentFileName = uiState.currentFile?.name ?: "index.html"
                val startFileName = if (currentFileName.endsWith(".html")) currentFileName else "index.html"
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
                            startFileName = startFileName,
                            projectId = projectId,
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
                        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
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
    }
    

    showRenameImageDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRenameImageDialog = null },
            title = { Text("Import Image") },
            text = {
                OutlinedTextField(
                    value = renameImageName,
                    onValueChange = { renameImageName = it },
                    label = { Text("Filename (e.g. hero.png)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameImageName.isNotBlank()) {
                        viewModel.importImage(uri, renameImageName)
                        showRenameImageDialog = null
                        Toast.makeText(context, "Image Imported", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameImageDialog = null }) {
                    Text("Cancel")
                }
            }
        )
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
    
    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Filename (e.g. style.css)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (fileName.isNotBlank()) {
                        viewModel.createFile(fileName)
                        showNewFileDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showRenameFileDialogFor?.let { fileToRename ->
        var renameFileTo by remember { mutableStateOf(fileToRename.name) }
        AlertDialog(
            onDismissRequest = { showRenameFileDialogFor = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameFileTo,
                    onValueChange = { renameFileTo = it },
                    label = { Text("New filename") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameFileTo.isNotBlank() && renameFileTo != fileToRename.name) {
                        viewModel.renameFile(fileToRename, renameFileTo)
                        Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                    }
                    showRenameFileDialogFor = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFileDialogFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit Project") },
            text = { Text("Are you sure you want to exit? Your changes are saved automatically.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    onNavigateBack()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Cancel")
                }
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
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
            
            HorizontalDivider()
            
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(logs) { log ->
                        val color = when (log.level) {
                            "ERROR" -> Color(0xFFCF6679)
                            "WARNING" -> Color(0xFFE6C74D)
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
                                    text = dateFormat.format(java.util.Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = color
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
