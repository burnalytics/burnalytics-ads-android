package com.burnalytics.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
public fun BurnalyticsBanner(
    slotID: String,
    modifier: Modifier = Modifier,
    onEvent: (BurnalyticsAdEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnEvent by rememberUpdatedState(onEvent)
    var ad by remember(slotID) { mutableStateOf<BurnalyticsAd?>(null) }
    var isLoading by remember(slotID) { mutableStateOf(true) }
    var creativeLoaded by remember(slotID) { mutableStateOf(false) }
    var trackedRequestID by remember(slotID) { mutableStateOf<String?>(null) }

    LaunchedEffect(slotID) {
        ad = null
        isLoading = true
        creativeLoaded = false
        trackedRequestID = null
        try {
            ad = BurnalyticsAdsClient.loadBanner(slotID)
        } catch (error: Throwable) {
            currentOnEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.from(error)))
        } finally {
            isLoading = false
        }
    }

    val loadedAd = ad
    if (loadedAd != null) {
        val creativeModifier = Modifier.fillMaxSize()
        val onCreativeLoad = {
            if (ad?.requestID == loadedAd.requestID) {
                creativeLoaded = true
                if (trackedRequestID != loadedAd.requestID) {
                    trackedRequestID = loadedAd.requestID
                    currentOnEvent(BurnalyticsAdEvent.Loaded)
                    currentOnEvent(BurnalyticsAdEvent.Impression)
                    scope.launch {
                        BurnalyticsAdsClient.recordImpression(
                            loadedAd.tracking.impressionURL,
                        )
                    }
                }
            }
        }
        val onCreativeFailure = {
            if (ad?.requestID == loadedAd.requestID) {
                ad = null
                creativeLoaded = false
                currentOnEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.CreativeFailed))
            }
        }

        Box(
            modifier = modifier.size(
                width = loadedAd.creative.width.dp,
                height = loadedAd.creative.height.dp,
            ),
        ) {
            when {
                loadedAd.creative.type == "html5" && loadedAd.creative.htmlURL != null -> {
                    BurnalyticsHTML5Creative(
                        url = loadedAd.creative.htmlURL,
                        modifier = creativeModifier,
                        onLoad = onCreativeLoad,
                        onFailure = onCreativeFailure,
                        onClick = { currentOnEvent(BurnalyticsAdEvent.Clicked) },
                    )
                }
                loadedAd.creative.imageURL != null -> {
                    BurnalyticsImageCreative(
                        imageURL = loadedAd.creative.imageURL,
                        alt = loadedAd.creative.alt,
                        modifier = creativeModifier,
                        onLoad = onCreativeLoad,
                        onFailure = onCreativeFailure,
                        onClick = {
                            currentOnEvent(BurnalyticsAdEvent.Clicked)
                            openExternalURL(context, loadedAd.creative.clickURL)
                        },
                    )
                }
                else -> LaunchedEffect(loadedAd.requestID) { onCreativeFailure() }
            }

            if (creativeLoaded) {
                BurnalyticsDisclosure(
                    disclosure = loadedAd.disclosure,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                )
            }
        }
    } else if (isLoading) {
        Box(modifier = modifier.size(width = 320.dp, height = 50.dp))
    }
}
