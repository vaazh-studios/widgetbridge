plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.vocabloot.widgetbridge.sample"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.vocabloot.widgetbridge.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    buildTypes {
        // Minified like a real release, so the sample proves the library's consumer keep rules
        // reach R8. Debug-signed so it installs anywhere.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// proguard-rules.pro asks R8 for `-printusage build/r8-usage.txt`, which CI greps to prove the
// library's consumer keep rules reached this build. R8 writes it as a side effect, so declare it
// as an output: otherwise a FROM-CACHE minifyReleaseWithR8 restores the APK and not the report.
tasks.matching { it.name == "minifyReleaseWithR8" }.configureEach {
    outputs.file(layout.buildDirectory.file("r8-usage.txt")).withPropertyName("r8UsageReport")
}

dependencies {
    implementation(project(":sample:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.glance.appwidget)
}
