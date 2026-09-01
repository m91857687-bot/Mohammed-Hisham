package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Base64
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewWebView(
    htmlContent: String,
    cssContent: String,
    jsContent: String,
    onConsoleMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val combinedHtml = remember(htmlContent, cssContent, jsContent) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                $cssContent
            </style>
        </head>
        <body>
            $htmlContent
            <script>
                // Catch errors
                window.onerror = function(message, source, lineno, colno, error) {
                    console.error(message + ' at line ' + lineno);
                    return true;
                };
                
                try {
                    $jsContent
                } catch(e) {
                    console.error(e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val encodedHtml = remember(combinedHtml) {
        Base64.encodeToString(combinedHtml.toByteArray(), Base64.NO_PADDING)
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            val msg = "${it.messageLevel().name}: ${it.message()} -- line ${it.lineNumber()}"
                            onConsoleMessage(msg)
                        }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadData(encodedHtml, "text/html", "base64")
        }
    )
}
