package com.burnalytics.ads

import org.json.JSONException
import org.json.JSONObject

internal data class BurnalyticsAd(
    val requestID: String,
    val format: String,
    val skipDelaySeconds: Int?,
    val creative: Creative,
    val tracking: Tracking,
    val disclosure: Disclosure,
) {
    data class Creative(
        val type: String,
        val imageURL: String?,
        val htmlURL: String?,
        val videoURL: String?,
        val clickURL: String,
        val width: Int,
        val height: Int,
        val alt: String,
        val durationSeconds: Double?,
    )

    data class Tracking(
        val impressionURL: String,
        val completionURL: String?,
    )

    data class Disclosure(
        val aboutURL: String,
        val label: String,
    )

    companion object {
        fun fromJSON(json: JSONObject): BurnalyticsAd {
            try {
                val creative = json.getJSONObject("creative")
                val tracking = json.getJSONObject("tracking")
                val disclosure = json.getJSONObject("disclosure")
                return BurnalyticsAd(
                    requestID = json.requiredString("request_id"),
                    format = json.requiredString("format"),
                    skipDelaySeconds = json.optionalInt("skip_delay_seconds"),
                    creative = Creative(
                        type = creative.requiredString("type"),
                        imageURL = creative.optionalString("image_url"),
                        htmlURL = creative.optionalString("html_url"),
                        videoURL = creative.optionalString("video_url"),
                        clickURL = creative.requiredString("click_url"),
                        width = creative.getInt("width").also { require(it > 0) },
                        height = creative.getInt("height").also { require(it > 0) },
                        alt = creative.requiredString("alt"),
                        durationSeconds = creative.optionalDouble("duration_seconds"),
                    ),
                    tracking = Tracking(
                        impressionURL = tracking.requiredString("impression_url"),
                        completionURL = tracking.optionalString("completion_url"),
                    ),
                    disclosure = Disclosure(
                        aboutURL = disclosure.requiredString("about_url"),
                        label = disclosure.requiredString("label"),
                    ),
                )
            } catch (error: Exception) {
                if (error is BurnalyticsAdsError) throw error
                throw BurnalyticsAdsError.InvalidResponse
            }
        }
    }
}

private fun JSONObject.requiredString(key: String): String =
    getString(key).trim().takeIf(String::isNotEmpty)
        ?: throw JSONException("$key is empty")

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf(String::isNotEmpty)

private fun JSONObject.optionalInt(key: String): Int? =
    if (isNull(key) || !has(key)) null else getInt(key)

private fun JSONObject.optionalDouble(key: String): Double? =
    if (isNull(key) || !has(key)) null else getDouble(key)

