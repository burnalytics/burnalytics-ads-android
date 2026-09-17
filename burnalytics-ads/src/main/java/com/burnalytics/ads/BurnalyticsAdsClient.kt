package com.burnalytics.ads

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object BurnalyticsAdsClient {
    private const val CACHE_LIFETIME_MILLIS = 5 * 60 * 1_000L
    private const val CONNECT_TIMEOUT_MILLIS = 10_000
    private const val READ_TIMEOUT_MILLIS = 20_000

    private data class Configuration(
        val appID: String,
        val packageName: String,
        val baseURL: String,
    )

    private data class CacheEntry(
        val ad: BurnalyticsAd,
        val loadedAtMillis: Long,
    )

    @Volatile
    private var configuration: Configuration? = null

    private val bannerCache = ConcurrentHashMap<String, CacheEntry>()
    private val interstitialCache = ConcurrentHashMap<String, CacheEntry>()
    private val rewardedCache = ConcurrentHashMap<String, CacheEntry>()

    fun configure(appID: String, packageName: String, baseURL: String) {
        configuration = Configuration(appID, packageName, baseURL)
        bannerCache.clear()
        interstitialCache.clear()
        rewardedCache.clear()
    }

    suspend fun loadBanner(slotID: String): BurnalyticsAd =
        loadCached(slotID, bannerCache) { ad -> ad.format == "banner" }

    suspend fun loadInterstitial(slotID: String): BurnalyticsAd =
        loadCached(slotID, interstitialCache) { ad ->
            ad.format == "interstitial_image" || ad.format == "interstitial_video"
        }

    suspend fun loadRewardedAd(slotID: String): BurnalyticsAd =
        loadCached(slotID, rewardedCache) { ad ->
            ad.format == "rewarded_video" && ad.creative.type == "video"
        }

    fun consumeInterstitial(slotID: String) {
        interstitialCache.remove(slotID)
    }

    fun consumeRewardedAd(slotID: String) {
        rewardedCache.remove(slotID)
    }

    suspend fun recordImpression(url: String) {
        recordTrackingEvent(url)
    }

    suspend fun recordCompletion(url: String) {
        recordTrackingEvent(url)
    }

    private suspend fun loadCached(
        slotID: String,
        cache: ConcurrentHashMap<String, CacheEntry>,
        accepts: (BurnalyticsAd) -> Boolean,
    ): BurnalyticsAd {
        val normalizedSlotID = slotID.trim()
        if (normalizedSlotID.isEmpty()) throw BurnalyticsAdsError.InvalidResponse
        val now = System.currentTimeMillis()
        cache[normalizedSlotID]
            ?.takeIf { now - it.loadedAtMillis < CACHE_LIFETIME_MILLIS }
            ?.let { return it.ad }

        val ad = requestAd(normalizedSlotID)
        if (!accepts(ad)) throw BurnalyticsAdsError.InvalidResponse
        cache[normalizedSlotID] = CacheEntry(ad, now)
        return ad
    }

    private suspend fun requestAd(slotID: String): BurnalyticsAd = withContext(Dispatchers.IO) {
        val config = configuration ?: throw BurnalyticsAdsError.NotConfigured
        if (config.appID.isEmpty() || config.packageName.isEmpty()) {
            throw BurnalyticsAdsError.NotConfigured
        }
        val payload = JSONObject()
            .put("app_id", config.appID)
            .put("slot_id", slotID)
            .put("bundle_id", config.packageName)
            .put("sdk_version", BurnalyticsAds.SDK_VERSION)
            .toString()

        val connection = openConnection("${config.baseURL}/api/sdk/v1/android/ads/request")
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "BurnalyticsAds-Android/${BurnalyticsAds.SDK_VERSION}",
            )
            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NO_CONTENT) {
                throw BurnalyticsAdsError.NoFill
            }
            val responseBody = readResponseBody(connection, status)
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(responseBody).optString("error", "Ad request failed.")
                }.getOrDefault("Ad request failed.")
                throw BurnalyticsAdsError.Server(message)
            }
            BurnalyticsAd.fromJSON(JSONObject(responseBody))
        } catch (error: BurnalyticsAdsError) {
            throw error
        } catch (error: IOException) {
            throw BurnalyticsAdsError.Network(
                error.message ?: "The ad request failed.",
                error,
            )
        } catch (error: Exception) {
            throw BurnalyticsAdsError.InvalidResponse
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun recordTrackingEvent(url: String): Unit = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection(url)
            try {
                connection.requestMethod = "POST"
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }
        Unit
    }

    private fun openConnection(url: String): HttpURLConnection {
        val uri = URI(url)
        if (uri.scheme != "https" && uri.scheme != "http") {
            throw BurnalyticsAdsError.InvalidResponse
        }
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            instanceFollowRedirects = true
            useCaches = false
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, status: Int): String {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }
}
