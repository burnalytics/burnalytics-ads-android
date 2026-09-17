package com.burnalytics.ads

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BurnalyticsAdTest {
    @Test
    fun parsesBannerResponse() {
        val ad = BurnalyticsAd.fromJSON(JSONObject(BANNER_RESPONSE))

        assertEquals("request-123", ad.requestID)
        assertEquals("banner", ad.format)
        assertEquals("image", ad.creative.type)
        assertEquals(320, ad.creative.width)
        assertEquals("https://example.com/banner.png", ad.creative.imageURL)
        assertNull(ad.tracking.completionURL)
    }

    @Test
    fun parsesRewardedResponse() {
        val ad = BurnalyticsAd.fromJSON(JSONObject(REWARDED_RESPONSE))

        assertEquals("rewarded_video", ad.format)
        assertEquals(15, ad.skipDelaySeconds)
        assertEquals("https://example.com/video.mp4", ad.creative.videoURL)
        assertEquals("https://example.com/completion", ad.tracking.completionURL)
    }

    @Test
    fun rejectsResponseWithInvalidDimensions() {
        val response = JSONObject(BANNER_RESPONSE)
        response.getJSONObject("creative").put("width", 0)

        assertThrows(BurnalyticsAdsError.InvalidResponse::class.java) {
            BurnalyticsAd.fromJSON(response)
        }
    }

    @Test
    fun rejectsResponseWithoutRequestID() {
        val response = JSONObject(BANNER_RESPONSE).apply { remove("request_id") }

        assertThrows(BurnalyticsAdsError.InvalidResponse::class.java) {
            BurnalyticsAd.fromJSON(response)
        }
    }

    private companion object {
        const val BANNER_RESPONSE = """
            {
              "request_id": "request-123",
              "format": "banner",
              "creative": {
                "type": "image",
                "image_url": "https://example.com/banner.png",
                "click_url": "https://example.com/click",
                "width": 320,
                "height": 50,
                "alt": "Example banner"
              },
              "tracking": {
                "impression_url": "https://example.com/impression"
              },
              "disclosure": {
                "about_url": "https://example.com/about",
                "label": "ADS"
              }
            }
        """

        const val REWARDED_RESPONSE = """
            {
              "request_id": "request-456",
              "format": "rewarded_video",
              "skip_delay_seconds": 15,
              "creative": {
                "type": "video",
                "video_url": "https://example.com/video.mp4",
                "click_url": "https://example.com/click",
                "width": 1080,
                "height": 1920,
                "alt": "Example rewarded ad",
                "duration_seconds": 30
              },
              "tracking": {
                "impression_url": "https://example.com/impression",
                "completion_url": "https://example.com/completion"
              },
              "disclosure": {
                "about_url": "https://example.com/about",
                "label": "ADS"
              }
            }
        """
    }
}
