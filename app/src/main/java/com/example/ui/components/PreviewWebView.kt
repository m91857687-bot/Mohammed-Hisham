package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.UUID

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewWebView(
    htmlContent: String,
    cssContent: String,
    jsContent: String,
    onConsoleMessage: (String, String) -> Unit, // Level, Message
    modifier: Modifier = Modifier,
    previewWidth: Int? = null,
    previewHeight: Int? = null,
    previewZoom: Float = 1f
) {
    val combinedHtml = remember(htmlContent, cssContent, jsContent) {
        val hasHtmlTag = htmlContent.contains("<html", ignoreCase = true)
        val hasHeadTag = htmlContent.contains("<head", ignoreCase = true)
        val hasBodyTag = htmlContent.contains("<body", ignoreCase = true)
        val hasMetaCharset = htmlContent.contains("<meta charset=", ignoreCase = true)
        val hasMetaViewport = htmlContent.contains("<meta name=\"viewport\"", ignoreCase = true)

        val metaCharset = if (!hasMetaCharset) "<meta charset=\"UTF-8\">" else ""
        val metaViewport = if (!hasMetaViewport) "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" else ""
        
        val styleTag = "<style>\n$cssContent\n</style>"
        val scriptTag = """
            <script>
                window.onerror = function(message, source, lineno, colno, error) {
                    console.error("Error: " + message + " at line " + lineno);
                    return true;
                };
                try {
                    $jsContent
                } catch(e) {
                    console.error("Runtime Error: " + e.message);
                }
            </script>
        """.trimIndent()

        if (hasHtmlTag) {
            var modifiedHtml = htmlContent
            
            // Inject meta, style, script
            if (hasHeadTag) {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$metaCharset\n$metaViewport\n$styleTag\n</head>")
            } else {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("(<html[^>]*>)", RegexOption.IGNORE_CASE), "$1\n<head>\n$metaCharset\n$metaViewport\n$styleTag\n</head>")
            }

            if (hasBodyTag) {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$scriptTag\n</body>")
            } else {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("</html>", RegexOption.IGNORE_CASE), "<body>\n$scriptTag\n</body>\n</html>")
            }
            modifiedHtml
        } else {
            """
            <!DOCTYPE html>
            <html lang="en" dir="auto">
            <head>
                $metaCharset
                $metaViewport
                $styleTag
            </head>
            <body>
                $htmlContent
                $scriptTag
            </body>
            </html>
            """.trimIndent()
        }
    }

    val encodedHtml = remember(combinedHtml) {
        Base64.encodeToString(combinedHtml.toByteArray(Charsets.UTF_8), Base64.NO_PADDING)
    }

    // A unique key for reload
    val webViewKey = remember(encodedHtml) { UUID.randomUUID().toString() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = previewZoom
                    scaleY = previewZoom
                }
                .then(
                    if (previewWidth != null && previewHeight != null) {
                        Modifier.width(previewWidth.dp).height(previewHeight.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .background(androidx.compose.ui.graphics.Color.White)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.defaultTextEncodingName = "UTF-8"
                        
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    val level = it.messageLevel().name
                                    val msg = "${it.message()} (Line ${it.lineNumber()})"
                                    onConsoleMessage(level, msg)
                                }
                                return true
                            }
                        }
                    }
                },
                update = { webView ->
                    // Adding a small delay or checking state helps avoid unnecessary reloads,
                    // but we're re-encoding.
                    webView.loadData(encodedHtml, "text/html; charset=utf-8", "base64")
                }
            )
        }
    }
}
