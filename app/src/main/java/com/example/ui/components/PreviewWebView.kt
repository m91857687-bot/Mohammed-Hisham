package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewWebView(
    projectId: Int,
    startFileName: String = "index.html",
    onConsoleMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    previewWidth: Int? = null,
    previewHeight: Int? = null,
    previewZoom: Float = 1f
) {
    val context = LocalContext.current
    val isSimulatedDevice = previewWidth != null && previewHeight != null
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    val projectDir = remember(projectId) { File(context.filesDir, "projects/project_$projectId") }
    val startUrl = "https://appassets.androidplatform.net/projects/project_$projectId/$startFileName"

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
                factory = { ctx ->
                    val assetLoader = WebViewAssetLoader.Builder()
                        .setDomain("appassets.androidplatform.net")
                        .addPathHandler(
                            "/projects/project_$projectId/",
                            WebViewAssetLoader.InternalStoragePathHandler(ctx, projectDir)
                        )
                        .build()

                    WebView(ctx).apply {
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
                            allowFileAccess = true
                            allowContentAccess = true
                            defaultTextEncodingName = "UTF-8"
                            
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = true
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request!!.url)
                            }
                        }
                        
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
                    webView.clearCache(true)
                    webView.loadUrl(startUrl)
                }
            )
        }
    }
}
