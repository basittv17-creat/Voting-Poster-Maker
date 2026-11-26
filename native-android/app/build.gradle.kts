import java.util.Properties

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlin.kapt") version "2.2.21"
}

val releaseSigningPath = providers.environmentVariable("VPM_RELEASE_SIGNING_FILE").orNull
val releaseSigningFile = releaseSigningPath?.let { rootProject.file(it) }
val releaseSigningProperties = releaseSigningFile?.let { credentials ->
    Properties().apply { credentials.inputStream().use { load(it) } }
}

android {
    namespace = "com.awisogera.votingpostermaker"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.awisogera.votingpostermaker"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "12.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    if (releaseSigningProperties != null && releaseSigningFile != null) {
        fun signingValue(name: String): String = requireNotNull(releaseSigningProperties.getProperty(name)
            ?.takeIf { it.isNotBlank() }) { "Missing release signing property: $name" }
        val production = signingConfigs.create("production") {
            storeFile = releaseSigningFile.parentFile.resolve(signingValue("storeFile"))
            storePassword = signingValue("storePassword")
            keyAlias = signingValue("keyAlias")
            keyPassword = signingValue("keyPassword")
        }
        buildTypes.named("release") { signingConfig = production }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    testImplementation("junit:junit:4.13.2")
}

// Exact legacy voting-frame PNGs are now committed under app/src/main/assets/frames/.
// The native Android build intentionally has no build-time dependency on www/index.html.
