sed -i '199,211c\
                            Row {\
                                IconButton(onClick = { showRenameFileDialogFor = file }) {\
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)\
                                }\
                                if (uiState.files.size > 1) {\
                                    IconButton(onClick = { viewModel.deleteFile(file) }) {\
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)\
                                    }\
                                }\
                            }\
                        }' app/src/main/java/com/example/ui/screens/EditorScreen.kt
