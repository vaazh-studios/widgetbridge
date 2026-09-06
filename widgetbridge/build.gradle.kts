import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)
}

group = "com.vocabloot"
version = "0.1.0"

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "com.vocabloot.widgetbridge"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(libs.coroutines.core)
            api(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "widgetbridge", version.toString())
    pom {
        name = "WidgetBridge"
        description = "Typed, atomic handoff of data and images from a Kotlin Multiplatform app to its Glance and WidgetKit widgets. The widget extension never links Kotlin."
        inceptionYear = "2026"
        url = "https://github.com/vaazh-studios/widgetbridge"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "vaazh-studios"
                name = "Vaazh Studios"
                url = "https://github.com/vaazh-studios"
            }
        }
        scm {
            url = "https://github.com/vaazh-studios/widgetbridge"
            connection = "scm:git:git://github.com/vaazh-studios/widgetbridge.git"
            developerConnection = "scm:git:ssh://git@github.com/vaazh-studios/widgetbridge.git"
        }
    }
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    klib { enabled = true }
}

dokka {
    moduleName.set("WidgetBridge")
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        sourceLink {
            localDirectory.set(file("src"))
            remoteUrl("https://github.com/vaazh-studios/widgetbridge/tree/main/widgetbridge/src")
            remoteLineSuffix.set("#L")
        }
    }
    pluginsConfiguration.html {
        footerMessage.set("WidgetBridge, Apache 2.0, Vaazh Studios")
    }
}
