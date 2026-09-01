package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    language: String,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    // Keep internal text field state independent to avoid cursor jumping
    var internalText by remember { mutableStateOf(TextFieldValue(code, TextRange(code.length))) }

    // Sync from outside if it actually changed outside (e.g. initial load or tab change)
    LaunchedEffect(code) {
        if (internalText.text != code) {
            internalText = TextFieldValue(code, TextRange(code.length))
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onKeyEvent { keyEvent ->
                if (keyEvent.isCtrlPressed && keyEvent.key == Key.S && keyEvent.type == KeyEventType.KeyUp) {
                    // We don't have direct access to save here, but AutoSave handles it.
                    // This is mainly to prevent default browser/system behavior.
                    true
                } else {
                    false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(verticalScrollState)
        ) {
            // Line Numbers
            Text(
                text = lineNumbers,
                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            )
            
            // Editor
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                BasicTextField(
                    value = internalText,
                    onValueChange = { newValue ->
                        val processedValue = handleAutoClosingAndIndent(internalText, newValue)
                        internalText = processedValue
                        onCodeChange(processedValue.text)
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

        // Quick Actions Toolbar
        ScrollableQuickActions { action ->
            val current = internalText
            val text = current.text
            val selStart = current.selection.start
            val selEnd = current.selection.end
            
            val newText = text.substring(0, selStart) + action + text.substring(selEnd)
            val newCursor = selStart + action.length
            val newValue = TextFieldValue(newText, TextRange(newCursor))
            
            internalText = newValue
            onCodeChange(newText)
        }
    }
}

private fun handleAutoClosingAndIndent(oldValue: TextFieldValue, newValue: TextFieldValue): TextFieldValue {
    // Very basic auto-closing
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

        // Auto indent on Enter
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
        val actions = listOf("<", ">", "/", "=", "{", "}", "(", ")", "[", "]", "\"", "'", ";", ":", "Tab")
        actions.forEach { action ->
            TextButton(
                onClick = { onAction(if (action == "Tab") "    " else action) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 36.dp)
            ) {
                Text(action, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
