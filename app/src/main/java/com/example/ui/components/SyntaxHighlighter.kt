package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlighter(private val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Prevent UI thread freezes on very large files by disabling highlighting
        if (text.text.length > 5000) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val highlighted = buildAnnotatedString {
            append(text.text)
            
            when (language) {
                "html" -> highlightHtml(this, text.text)
                "css" -> highlightCss(this, text.text)
                "js" -> highlightJs(this, text.text)
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    private fun highlightHtml(builder: AnnotatedString.Builder, text: String) {
        val tagRegex = Regex("<[^>]*>")
        val attrRegex = Regex("([a-zA-Z\\-]+)\\s*=\\s*(\"[^\"]*\"|'[^']*')")
        
        tagRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFF7CD389)), match.range.first, match.range.last + 1)
        }
        
        attrRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFF91A7FF)), match.groups[1]!!.range.first, match.groups[1]!!.range.last + 1)
            builder.addStyle(SpanStyle(color = Color(0xFFCE9178)), match.groups[2]!!.range.first, match.groups[2]!!.range.last + 1)
        }
    }

    private fun highlightCss(builder: AnnotatedString.Builder, text: String) {
        val propertyRegex = Regex("([a-zA-Z\\-]+)\\s*:")
        val valueRegex = Regex(":\\s*([^;]+);")
        val classRegex = Regex("\\.[a-zA-Z0-9_\\-]+")
        val idRegex = Regex("#[a-zA-Z0-9_\\-]+")

        propertyRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFF91A7FF)), match.groups[1]!!.range.first, match.groups[1]!!.range.last + 1)
        }
        valueRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFFCE9178)), match.groups[1]!!.range.first, match.groups[1]!!.range.last + 1)
        }
        classRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFFD0BCFF)), match.range.first, match.range.last + 1)
        }
        idRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFFD0BCFF)), match.range.first, match.range.last + 1)
        }
    }

    private fun highlightJs(builder: AnnotatedString.Builder, text: String) {
        val keywords = listOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "class", "import", "export")
        val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
        val stringRegex = Regex("(\"[^\"]*\"|'[^']*'|`[^`]*`)")
        val functionRegex = Regex("\\b([a-zA-Z0-9_]+)\\s*\\(")

        keywordRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFFD0BCFF)), match.range.first, match.range.last + 1)
        }
        stringRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFFCE9178)), match.range.first, match.range.last + 1)
        }
        functionRegex.findAll(text).forEach { match ->
            builder.addStyle(SpanStyle(color = Color(0xFF7CD389)), match.groups[1]!!.range.first, match.groups[1]!!.range.last + 1)
        }
    }
}
