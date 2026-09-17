package com.burnalytics.ads

public sealed interface BurnalyticsAdEvent {
    public data object Loaded : BurnalyticsAdEvent
    public data object Impression : BurnalyticsAdEvent
    public data object Clicked : BurnalyticsAdEvent
    public data object Skipped : BurnalyticsAdEvent
    public data object Dismissed : BurnalyticsAdEvent
    public data class Failed(val error: BurnalyticsAdsError) : BurnalyticsAdEvent
    public data object Rewarded : BurnalyticsAdEvent
}

