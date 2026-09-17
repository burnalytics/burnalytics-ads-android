package com.burnalytics.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
public fun BurnalyticsInterstitial(
    slotID: String,
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    onEvent: (BurnalyticsAdEvent) -> Unit = {},
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var ad by remember(slotID) { mutableStateOf<BurnalyticsAd?>(null) }

    LaunchedEffect(isPresented, slotID) {
        if (isPresented && ad == null) {
            try {
                ad = BurnalyticsAdsClient.loadInterstitial(slotID)
                currentOnEvent(BurnalyticsAdEvent.Loaded)
            } catch (error: Throwable) {
                currentOnEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.from(error)))
                currentOnDismissRequest()
            }
        } else if (!isPresented && ad != null) {
            currentOnEvent(BurnalyticsAdEvent.Dismissed)
            BurnalyticsAdsClient.consumeInterstitial(slotID)
            ad = null
        }
    }

    if (isPresented) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                ad?.let { loadedAd ->
                    BurnalyticsInterstitialContent(
                        ad = loadedAd,
                        onDismissRequest = currentOnDismissRequest,
                        onEvent = currentOnEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun BurnalyticsInterstitialContent(
    ad: BurnalyticsAd,
    onDismissRequest: () -> Unit,
    onEvent: (BurnalyticsAdEvent) -> Unit,
) {
    val context = LocalContext.current
    var secondsRemaining by remember(ad.requestID) { mutableIntStateOf(3) }
    var failed by remember(ad.requestID) { mutableStateOf(false) }

    LaunchedEffect(ad.requestID) {
        onEvent(BurnalyticsAdEvent.Impression)
        launch { BurnalyticsAdsClient.recordImpression(ad.tracking.impressionURL) }
        while (secondsRemaining > 0) {
            delay(1_000)
            secondsRemaining -= 1
        }
    }

    val creativeFailure = {
        if (!failed) {
            failed = true
            onEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.CreativeFailed))
            onDismissRequest()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            ad.creative.type == "video" && ad.creative.videoURL != null -> {
                BurnalyticsVideoCreative(
                    url = ad.creative.videoURL,
                    muted = true,
                    modifier = Modifier.fillMaxSize(),
                    onFailure = creativeFailure,
                )
            }
            ad.creative.imageURL != null -> {
                BurnalyticsImageCreative(
                    imageURL = ad.creative.imageURL,
                    alt = ad.creative.alt,
                    modifier = Modifier.fillMaxSize(),
                    onLoad = {},
                    onFailure = creativeFailure,
                    onClick = {
                        onEvent(BurnalyticsAdEvent.Clicked)
                        openExternalURL(context, ad.creative.clickURL)
                    },
                )
            }
            else -> LaunchedEffect(ad.requestID) { creativeFailure() }
        }

        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BurnalyticsDisclosure(ad.disclosure)
                Spacer(Modifier.weight(1f))
                BurnalyticsRoundControl(
                    text = if (secondsRemaining > 0) secondsRemaining.toString() else "×",
                    enabled = secondsRemaining <= 0,
                    contentDescription = if (secondsRemaining > 0) {
                        "Close available in $secondsRemaining seconds"
                    } else {
                        "Close ad"
                    },
                    onClick = onDismissRequest,
                )
            }

            if (ad.creative.type == "video") {
                Box(Modifier.padding(16.dp)) {
                    BurnalyticsLearnMoreButton {
                        onEvent(BurnalyticsAdEvent.Clicked)
                        openExternalURL(context, ad.creative.clickURL)
                    }
                }
            }
        }
    }
}
