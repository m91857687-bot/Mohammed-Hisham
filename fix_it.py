import re

with open("app/src/main/java/com/example/ui/screens/EditorScreen.kt", "r") as f:
    text = f.read()

# The file contains multiple `showRenameFileDialogFor?.let` and `ConsoleDialog`.
# Let's find the end of `Scaffold`.

# We know `Scaffold` ends with:
#                 }
#             }
#         }
#     }
# 
#     showRenameImageDialog?.let { uri ->

scaffold_end_idx = text.find("    showRenameImageDialog?.let { uri ->")
if scaffold_end_idx == -1:
    print("Could not find Scaffold end!")
    # Let's try to find it by regex
    pass

text_before = text[:scaffold_end_idx]

# Let's make sure it closes `EditorScreen` properly.
# The `EditorScreen` composable should end with `}`.
# Currently `text_before` does not have the final `}` for `EditorScreen`.
# Let's add the dialogs and then the final `}`.

dialogs = """
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
                val allText = consoleLogs.joinToString("\\n") { "[${it.level}] ${it.message}" }
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
"""

console_dialog = """
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
"""

with open("app/src/main/java/com/example/ui/screens/EditorScreen.kt", "w") as f:
    f.write(text_before + dialogs + console_dialog)

print("Done fixing!")
