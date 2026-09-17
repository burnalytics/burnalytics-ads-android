package com.burnalytics.ads

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Success(val bitmap: Bitmap) : ImageLoadState
    data object Failed : ImageLoadState
}

@Composable
internal fun BurnalyticsImageCreative(
    imageURL: String,
    alt: String,
    modifier: Modifier = Modifier,
    onLoad: () -> Unit,
    onFailure: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val state by produceState<ImageLoadState>(ImageLoadState.Loading, imageURL) {
        value = withContext(Dispatchers.IO) {
            runCatching { downloadBitmap(imageURL) }
                .fold(
                    onSuccess = { bitmap ->
                        bitmap?.let(ImageLoadState::Success) ?: ImageLoadState.Failed
                    },
                    onFailure = { ImageLoadState.Failed },
                )
        }
    }

    when (val current = state) {
        ImageLoadState.Loading -> Box(modifier)
        is ImageLoadState.Success -> {
            androidx.compose.runtime.LaunchedEffect(current.bitmap) { onLoad() }
            Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = alt,
                modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
                contentScale = ContentScale.Fit,
            )
        }
        ImageLoadState.Failed -> {
            androidx.compose.runtime.LaunchedEffect(imageURL) { onFailure() }
            Box(modifier)
        }
    }
}

@Composable
internal fun BurnalyticsHTML5Creative(
    url: String,
    modifier: Modifier = Modifier,
    onLoad: () -> Unit,
    onFailure: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnLoad by rememberUpdatedState(onLoad)
    val currentOnFailure by rememberUpdatedState(onFailure)
    val currentOnClick by rememberUpdatedState(onClick)
    val webView = remember(url) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, loadedURL: String) {
                    currentOnLoad()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) currentOnFailure()
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = handleNavigation(context, request.url, url, currentOnClick)

                @Deprecated("Required for Android versions below API 24")
                override fun shouldOverrideUrlLoading(view: WebView, destination: String): Boolean =
                    handleNavigation(context, Uri.parse(destination), url, currentOnClick)
            }
            loadUrl(url)
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }
}

@Composable
internal fun BurnalyticsVideoCreative(
    url: String,
    muted: Boolean,
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {},
    onCompleted: () -> Unit = {},
    onFailure: () -> Unit = {},
) {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val currentOnFailure by rememberUpdatedState(onFailure)
    val videoView = remember(url) {
        VideoView(context).apply {
            setVideoURI(Uri.parse(url))
            setOnPreparedListener { player ->
                player.setVolume(if (muted) 0f else 1f, if (muted) 0f else 1f)
                currentOnReady()
                start()
            }
            setOnCompletionListener { currentOnCompleted() }
            setOnErrorListener { _, _, _ ->
                currentOnFailure()
                true
            }
        }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier.background(Color.Black),
        update = { view -> if (!view.isPlaying) view.start() },
    )
    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }
}

@Composable
internal fun BurnalyticsDisclosure(
    disclosure: BurnalyticsAd.Disclosure,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(50))
            .border(1.dp, Color(0xFFE6A700), RoundedCornerShape(50))
            .clickable { openExternalURL(context, disclosure.aboutURL) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = disclosure.label,
            style = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

internal fun openExternalURL(context: Context, url: String) {
    val uri = runCatching { URI(url) }.getOrNull() ?: return
    if (uri.scheme != "https" && uri.scheme != "http") return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun handleNavigation(
    context: Context,
    destination: Uri,
    creativeURL: String,
    onClick: () -> Unit,
): Boolean {
    val destinationText = destination.toString()
    if (destinationText == creativeURL || destination.scheme == "about") return false
    if (destination.scheme == "https" || destination.scheme == "http") {
        onClick()
        openExternalURL(context, destinationText)
    }
    return true
}

private fun downloadBitmap(url: String): Bitmap? {
    val uri = URI(url)
    if (uri.scheme != "https" && uri.scheme != "http") return null
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        connection.inputStream.use(BitmapFactory::decodeStream)
    } finally {
        connection.disconnect()
    }
}
