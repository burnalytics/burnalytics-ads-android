package com.burnalytics.ads

import android.content.Context
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

public object BurnalyticsAds {
    public const val SDK_VERSION: String = "1.0.0"

    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Configure the SDK once, normally from Application.onCreate(). */
    @JvmOverloads
    public fun configure(
        context: Context,
        appID: String,
        baseURL: String = "https://www.burnalytics.com",
    ) {
        val normalizedBaseURL = baseURL.trim().trimEnd('/')
        require(normalizedBaseURL.isNotEmpty()) { "baseURL cannot be empty." }
        val uri = URI(normalizedBaseURL)
        require(uri.scheme == "https" || uri.scheme == "http") {
            "baseURL must use HTTP or HTTPS."
        }
        BurnalyticsAdsClient.configure(
            appID = appID.trim(),
            packageName = context.applicationContext.packageName,
            baseURL = normalizedBaseURL,
        )
    }

    public fun preloadBanner(slotID: String) {
        preloadScope.launch { runCatching { BurnalyticsAdsClient.loadBanner(slotID) } }
    }

    public fun preloadInterstitial(slotID: String) {
        preloadScope.launch { runCatching { BurnalyticsAdsClient.loadInterstitial(slotID) } }
    }

    public fun preloadRewardedAd(slotID: String) {
        preloadScope.launch { runCatching { BurnalyticsAdsClient.loadRewardedAd(slotID) } }
    }
}
