package com.burnalytics.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
public fun BurnalyticsRewardedAd(
    slotID: String,
    isPresented: Boolean,
    onDismissRequest: () -> Unit,
    onEvent: (BurnalyticsAdEvent) -> Unit = {},
    onReward: () -> Unit,
) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentOnReward by rememberUpdatedState(onReward)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var ad by remember(slotID) { mutableStateOf<BurnalyticsAd?>(null) }

    LaunchedEffect(isPresented, slotID) {
        if (isPresented && ad == null) {
            try {
                ad = BurnalyticsAdsClient.loadRewardedAd(slotID)
                currentOnEvent(BurnalyticsAdEvent.Loaded)
            } catch (error: Throwable) {
                currentOnEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.from(error)))
                currentOnDismissRequest()
            }
        } else if (!isPresented && ad != null) {
            currentOnEvent(BurnalyticsAdEvent.Dismissed)
            BurnalyticsAdsClient.consumeRewardedAd(slotID)
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
                    BurnalyticsRewardedContent(
                        ad = loadedAd,
                        onDismissRequest = currentOnDismissRequest,
                        onEvent = currentOnEvent,
                        onReward = currentOnReward,
                    )
                }
            }
        }
    }
}

@Composable
private fun BurnalyticsRewardedContent(
    ad: BurnalyticsAd,
    onDismissRequest: () -> Unit,
    onEvent: (BurnalyticsAdEvent) -> Unit,
    onReward: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialSkipDelay = (ad.skipDelaySeconds ?: 5).coerceAtLeast(0)
    var secondsUntilSkip by remember(ad.requestID) {
        mutableStateOf(initialSkipDelay.takeUnless { it == 0 })
    }
    var rewardGranted by remember(ad.requestID) { mutableStateOf(false) }
    var failed by remember(ad.requestID) { mutableStateOf(false) }

    LaunchedEffect(ad.requestID) {
        onEvent(BurnalyticsAdEvent.Impression)
        launch { BurnalyticsAdsClient.recordImpression(ad.tracking.impressionURL) }
        while ((secondsUntilSkip ?: 0) > 0) {
            delay(1_000)
            secondsUntilSkip = secondsUntilSkip?.minus(1)
        }
    }

    val creativeFailure = {
        if (!failed) {
            failed = true
            onEvent(BurnalyticsAdEvent.Failed(BurnalyticsAdsError.CreativeFailed))
            onDismissRequest()
        }
    }
    val grantReward = {
        if (!rewardGranted) {
            rewardGranted = true
            scope.launch {
                ad.tracking.completionURL?.let {
                    BurnalyticsAdsClient.recordCompletion(it)
                }
                onEvent(BurnalyticsAdEvent.Rewarded)
                onReward()
                onDismissRequest()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val videoURL = ad.creative.videoURL
        if (videoURL != null) {
            BurnalyticsVideoCreative(
                url = videoURL,
                muted = false,
                modifier = Modifier.fillMaxSize(),
                onCompleted = grantReward,
                onFailure = creativeFailure,
            )
        } else {
            LaunchedEffect(ad.requestID) { creativeFailure() }
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
                secondsUntilSkip?.let { seconds ->
                    BurnalyticsSkipControl(secondsRemaining = seconds) {
                        onEvent(BurnalyticsAdEvent.Skipped)
                        onDismissRequest()
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(
                    text = "Watch the full video to earn your reward",
                    modifier = Modifier
                        .background(Color(0xA6000000), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Box(Modifier.padding(top = 10.dp)) {
                    BurnalyticsLearnMoreButton {
                        onEvent(BurnalyticsAdEvent.Clicked)
                        openExternalURL(context, ad.creative.clickURL)
                    }
                }
            }
        }
    }
}
