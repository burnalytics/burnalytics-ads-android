# Burnalytics Ads SDK for Android

Official Burnalytics Ads SDK for Android apps. The SDK is written in Kotlin and provides Jetpack Compose components for banner, HTML5, image interstitial, video interstitial, and rewarded video ads.

## Requirements

- Android 6.0 (API 23) or newer
- Kotlin 2.3+
- Jetpack Compose
- Java 17+

## Installation

The SDK is not published to a Maven repository yet. From a local checkout, publish it to your local Maven repository:

```shell
./gradlew publishToMavenLocal
```

Add `mavenLocal()` to your app's repositories, then add the library to your app module:

```kotlin
dependencies {
    implementation("com.burnalytics:burnalytics-ads:1.0.0")
}
```

## Configure

Configure the SDK once from your `Application` class:

```kotlin
import android.app.Application
import com.burnalytics.ads.BurnalyticsAds

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BurnalyticsAds.configure(
            context = this,
            appID = "app-1669474039",
        )
    }
}
```

Use the App ID from your Burnalytics publisher dashboard.

## Banner Ads

Add a banner placement anywhere in Compose:

```kotlin
import androidx.compose.runtime.Composable
import com.burnalytics.ads.BurnalyticsBanner

@Composable
fun Content() {
    BurnalyticsBanner(slotID = "4165549702")
}
```

Banner slots can serve image or HTML5 ads, depending on the publisher slot settings in Burnalytics.

## Interstitial Ads

Keep the component in your composition and control it with state:

```kotlin
var showInterstitial by remember { mutableStateOf(false) }

Button(onClick = { showInterstitial = true }) {
    Text("Show interstitial")
}

BurnalyticsInterstitial(
    slotID = "2911997583",
    isPresented = showInterstitial,
    onDismissRequest = { showInterstitial = false },
)
```

Use an image interstitial slot for image creatives or a video interstitial slot for video creatives.

## Rewarded Video Ads

The reward callback runs only after the video reaches the end. Closing or skipping the ad does not grant the reward.

```kotlin
var showRewardedAd by remember { mutableStateOf(false) }
var coins by remember { mutableIntStateOf(0) }

Button(onClick = { showRewardedAd = true }) {
    Text("Watch ad for 10 coins")
}

BurnalyticsRewardedAd(
    slotID = "YOUR_REWARDED_SLOT_ID",
    isPresented = showRewardedAd,
    onDismissRequest = { showRewardedAd = false },
    onEvent = { event ->
        if (event is BurnalyticsAdEvent.Skipped) {
            println("The user skipped without earning a reward")
        }
    },
    onReward = { coins += 10 },
)
```

## Preloading

Preload ads before showing them:

```kotlin
BurnalyticsAds.preloadBanner("4165549702")
BurnalyticsAds.preloadInterstitial("5207566349")
BurnalyticsAds.preloadRewardedAd("YOUR_REWARDED_SLOT_ID")
```

## Lifecycle Events

All formats support a typed event callback:

```kotlin
BurnalyticsBanner(slotID = "4165549702") { event ->
    when (event) {
        BurnalyticsAdEvent.Loaded -> println("Banner loaded")
        BurnalyticsAdEvent.Impression -> println("Banner impression")
        BurnalyticsAdEvent.Clicked -> println("Banner clicked")
        is BurnalyticsAdEvent.Failed -> println(event.error.message)
        else -> Unit
    }
}
```

Supported events are `Loaded`, `Impression`, `Clicked`, `Skipped`, `Dismissed`, `Failed`, and `Rewarded`.

## Test IDs

Use the IDs from your Burnalytics publisher dashboard:

```text
App ID: app-1669474039
Banner Slot ID: 4165549702
Image Interstitial Slot ID: 2911997583
Video Interstitial Slot ID: 5207566349
```

## Notes

- Impression tracking is handled automatically.
- Interstitial close controls are handled by the SDK.
- Reward completion tracking is sent before the app reward callback runs.
- Rewarded-video Skip timing is controlled by the server zone.
- HTML5 ads use an isolated WebView with storage, cache, file access, and content access disabled.
- The SDK does not request advertising identifiers and does not track users across apps.

## License

MIT
