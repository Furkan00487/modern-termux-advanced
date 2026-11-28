package com.termux.app.settings.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.termux.R

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxWebViewerScreen(
    initialTitle: String,
    initialUrl: String,
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var pageTitle by remember(initialUrl) { mutableStateOf(initialTitle) }
    var loadProgress by remember(initialUrl) { mutableIntStateOf(0) }
    var canGoBack by remember(initialUrl) { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = true
                }
            }
        }
    }

    webView.webViewClient = remember(onOpenInBrowser) {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val targetUrl = request?.url?.toString() ?: return false
                if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                    currentUrl = targetUrl
                    return false
                }
                onOpenInBrowser(targetUrl)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (!url.isNullOrBlank()) {
                    currentUrl = url
                }
                canGoBack = view?.canGoBack() == true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!url.isNullOrBlank()) {
                    currentUrl = url
                }
                canGoBack = view?.canGoBack() == true
                loadProgress = 100
            }
        }
    }

    webView.webChromeClient = remember {
        object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadProgress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    pageTitle = title
                }
            }
        }
    }

    LaunchedEffect(initialUrl) {
        webView.loadUrl(initialUrl)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    val webBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = webBackState,
        isBackEnabled = canGoBack,
        onBackCompleted = { webView.goBack() },
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_viewer_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (webView.canGoBack()) {
                                    webView.goBack()
                                } else {
                                    onBack()
                                }
                            },
                            modifier = Modifier.testTag("web_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = pageTitle.ifBlank { initialTitle },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val hostLabel = remember(currentUrl) {
                                Uri.parse(currentUrl).host?.takeIf { it.isNotBlank() } ?: currentUrl
                            }
                            Text(
                                text = hostLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onOpenInBrowser(currentUrl) },
                            modifier = Modifier.testTag("web_open_in_browser"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_web_open_in_browser),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
                AnimatedVisibility(
                    visible = loadProgress in 1..99,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { innerPadding ->
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
