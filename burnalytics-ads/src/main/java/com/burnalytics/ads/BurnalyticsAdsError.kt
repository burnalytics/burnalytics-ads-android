package com.burnalytics.ads

public sealed class BurnalyticsAdsError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public data object NotConfigured : BurnalyticsAdsError(
        "Burnalytics Ads has not been configured.",
    )

    public data object InvalidResponse : BurnalyticsAdsError(
        "The ad server returned an invalid response.",
    )

    public data object NoFill : BurnalyticsAdsError(
        "No ad is available for this placement.",
    )

    public data object CreativeFailed : BurnalyticsAdsError(
        "The ad creative could not be displayed.",
    )

    public class Network(message: String, cause: Throwable? = null) :
        BurnalyticsAdsError(message, cause)

    public class Server(message: String) : BurnalyticsAdsError(message)

    internal companion object {
        fun from(error: Throwable): BurnalyticsAdsError =
            error as? BurnalyticsAdsError
                ?: Network(error.message ?: "The ad request failed.", error)
    }
}

