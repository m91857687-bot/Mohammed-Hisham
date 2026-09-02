package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    language: String,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    var internalText by remember { mutableStateOf(TextFieldValue(code, TextRange(code.length))) }
    
    // Undo / Redo stacks
    var undoStack by remember { mutableStateOf(listOf(internalText)) }
    var redoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    
    // Search feature
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(code) {
        if (internalText.text != code) {
            internalText = TextFieldValue(code, TextRange(code.length))
            // Reset stacks on external file change
            undoStack = listOf(internalText)
            redoStack = emptyList()
        }
    }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    
    val lineCount = remember(internalText.text) { internalText.text.count { it == '\n' } + 1 }
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }
    
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = (fontSize * 1.5).sp
    )
    
    // A helper to push state to Undo stack with basic debouncing
    fun updateTextWithHistory(newValue: TextFieldValue) {
        if (internalText.text != newValue.text) {
            val last = undoStack.lastOrNull()
            if (last?.text != internalText.text) {
                 undoStack = (undoStack + internalText).takeLast(20) // keep last 20 states
            }
            redoStack = emptyList()
            internalText = newValue
            onCodeChange(newValue.text)
        } else {
            internalText = newValue // just cursor change
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar for Undo/Redo/Search
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (undoStack.isNotEmpty()) {
                    val prev = undoStack.last()
                    undoStack = undoStack.dropLast(1)
                    redoStack = redoStack + internalText
                    internalText = prev
                    onCodeChange(prev.text)
                }
            }, enabled = undoStack.isNotEmpty()) {
                Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray)
            }
            IconButton(onClick = {
                if (redoStack.isNotEmpty()) {
                    val next = redoStack.last()
                    redoStack = redoStack.dropLast(1)
                    undoStack = undoStack + internalText
                    internalText = next
                    onCodeChange(next.text)
                }
            }, enabled = redoStack.isNotEmpty()) {
                Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray)
            }
            IconButton(onClick = { showSearch = !showSearch }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            }
            // Add Color snippet helper
            IconButton(onClick = { 
                val text = internalText.text
                showColorPicker = true
            }) {
                Icon(Icons.Default.Palette, contentDescription = "Insert Color", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        if (showSearch) {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Find") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        label = { Text("Replace") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (searchQuery.isNotEmpty()) {
                            val newText = internalText.text.replace(searchQuery, replaceQuery)
                            updateTextWithHistory(TextFieldValue(newText, TextRange(newText.length)))
                        }
                    }) {
                        Text("Replace All")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(verticalScrollState)
        ) {
            Text(
                text = lineNumbers,
                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                BasicTextField(
                    value = internalText,
                    onValueChange = { newValue ->
                        val processedValue = handleAutoClosingAndIndent(internalText, newValue)
                        updateTextWithHistory(processedValue)
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = SyntaxHighlighter(language),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .horizontalScroll(horizontalScrollState)
                        .focusRequester(focusRequester)
                )
            }
        }

        if (showColorPicker) {
            ColorPickerDialog(
                onColorSelected = { hex ->
                    showColorPicker = false
                    val text = internalText.text
                    val sel = internalText.selection.start
                    val newValue = TextFieldValue(text.substring(0, sel) + hex + text.substring(internalText.selection.end), TextRange(sel + hex.length))
                    updateTextWithHistory(newValue)
                },
                onDismiss = { showColorPicker = false }
            )
        }

        ScrollableQuickActions { action ->
            val current = internalText
            val text = current.text
            val selStart = current.selection.start
            val selEnd = current.selection.end
            
            val newText = text.substring(0, selStart) + action + text.substring(selEnd)
            val newCursor = selStart + action.length
            
            updateTextWithHistory(TextFieldValue(newText, TextRange(newCursor)))
        }
    }
}

private fun handleAutoClosingAndIndent(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
    if (newValue.text.length == oldValue.text.length + 1) {
        val insertedChar = newValue.text[newValue.selection.start - 1]
        val closingChar = when (insertedChar) {
            '{' -> '}'
            '[' -> ']'
            '(' -> ')'
            '"' -> '"'
            '\'' -> '\''
            else -> null
        }
        if (closingChar != null) {
            val text = newValue.text
            val sel = newValue.selection.start
            val newText = text.substring(0, sel) + closingChar + text.substring(sel)
            return TextFieldValue(newText, TextRange(sel))
        }
        if (insertedChar == '\n') {
            val sel = newValue.selection.start
            val textBeforeEnter = newValue.text.substring(0, sel - 1)
            val lastLineStart = textBeforeEnter.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
            val lastLine = textBeforeEnter.substring(lastLineStart)
            val leadingSpaces = lastLine.takeWhile { it == ' ' || it == '\t' }
            if (leadingSpaces.isNotEmpty()) {
                val newText = newValue.text.substring(0, sel) + leadingSpaces + newValue.text.substring(sel)
                return TextFieldValue(newText, TextRange(sel + leadingSpaces.length))
            }
        }
    }
    return newValue
}

@Composable
fun ScrollableQuickActions(onAction: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val actions = listOf(
            "<", ">", "/", "=", "{", "}", "(", ")", "[", "]", "\"", "'", ";", ":", 
            "Tab", "div", "button", "console.log"
        )
        actions.forEach { action ->
            TextButton(
                onClick = { 
                    val insertText = when (action) {
                        "Tab" -> "    "
                        "div" -> "<div></div>"
                        "button" -> "<button></button>"
                        "console.log" -> "console.log();"
                        else -> action
                    }
                    onAction(insertText) 
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 36.dp)
            ) {
                Text(action, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(onColorSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(
        "#FF5733", "#33FF57", "#3357FF", "#F1C40F", "#9B59B6", "#E74C3C", 
        "#1ABC9C", "#2ECC71", "#3498DB", "#34495E", "#FFFFFF", "#000000",
        "#E67E22", "#D35400", "#C0392B", "#7F8C8D"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Color") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // simple 4x4 grid
                for (i in 0 until 4) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (j in 0 until 4) {
                            val hex = colors[i * 4 + j]
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { onColorSelected(hex) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
