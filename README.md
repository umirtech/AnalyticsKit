# AnalyticsKit

Lightweight Firebase Analytics wrapper for Android apps.

## Installation

Add JitPack to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.umirtech:AnalyticsKit:1.0.0")
}
```

## Usage

Initialize once from `Application.onCreate`, then log events:

```kotlin
AnalyticsKit.init(context)
AnalyticsKit.applyConsentDefaults()

AnalyticsKit.logEvent(
    "screen_view",
    params = mapOf("screen_name" to "home"),
)
AnalyticsKit.logScreenView("home")
```

See the source for the full API.
