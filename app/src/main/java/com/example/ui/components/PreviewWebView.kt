package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewWebView(
    htmlContent: String,
    cssContent: String,
    jsContent: String,
    onConsoleMessage: (String, String) -> Unit,
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

        val metaCharset = if (!hasMetaCharset) "<meta charset=\"UTF-8\">\n" else ""
        val metaViewport = if (!hasMetaViewport) "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=5.0\">\n" else ""
        
        val styleTag = "<style>\n$cssContent\n</style>\n"
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
            if (hasHeadTag) {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$metaCharset$metaViewport$styleTag</head>")
            } else {
                modifiedHtml = modifiedHtml.replaceFirst(Regex("(<html[^>]*>)", RegexOption.IGNORE_CASE), "$1\n<head>\n$metaCharset$metaViewport$styleTag</head>")
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
            <html lang="ar" dir="auto">
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

    val isSimulatedDevice = previewWidth != null && previewHeight != null
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (isSimulatedDevice) {
                    Modifier
                        .verticalScroll(verticalScroll)
                        .horizontalScroll(horizontalScroll)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = previewZoom
                    scaleY = previewZoom
                }
                .then(
                    if (isSimulatedDevice) {
                        Modifier.width(previewWidth!!.dp).height(previewHeight!!.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        clipToPadding = false
                        clipChildren = false
                        setBackgroundColor(Color.TRANSPARENT)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            defaultTextEncodingName = "UTF-8"
                            
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        
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
                    webView.loadDataWithBaseURL("http://localhost/", combinedHtml, "text/html", "UTF-8", null)
                }
            )
        }
    }
}
