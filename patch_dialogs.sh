sed -i '/val project = uiState.project ?: return/a \    BackHandler {\n        if (uiState.isPreviewing) {\n            viewModel.togglePreview()\n        } else {\n            showExitConfirmation = true\n        }\n    }' app/src/main/java/com/example/ui/screens/EditorScreen.kt

cat << 'INNEREOF' >> app/src/main/java/com/example/ui/screens/EditorScreen.kt

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
INNEREOF
