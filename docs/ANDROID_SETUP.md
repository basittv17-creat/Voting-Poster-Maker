# Android setup

Open `native-android` in Android Studio. Install Android SDK 36 and build tools 36.0.0, and use a full JDK 17 or newer. Run from that directory:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Pinned stack: Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin and Compose compiler plugin 2.2.21, Compose BOM 2025.10.01, Room 2.7.2, DataStore 1.1.7, ML Kit Selfie Segmentation 16.0.0-beta6. The compatibility floor remains API 24 and the target remains API 36.

This project builds separately from the Capacitor project in `android/`. The debug APK uses the existing application ID with a debug certificate, so use a separate test device/profile when a production-signed copy is already installed.

[Android Gradle plugin compatibility](https://developer.android.com/build/releases/about-agp) · [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
