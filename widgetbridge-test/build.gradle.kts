import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)
}

group = "com.vocabloot"
version = "0.2.0"

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "com.vocabloot.widgetbridge.test"
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
            api(project(":widgetbridge"))
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "widgetbridge-test", version.toString())
    pom {
        name = "WidgetBridge Test"
        description = "In-memory fakes for testing code built on WidgetBridge: FakeWidgetFeedStorage and CountingNotifier."
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
    moduleName.set("WidgetBridge Test")
    dokkaSourceSets.configureEach {
                sourceLink {
            localDirectory.set(file("src"))
            remoteUrl("https://github.com/vaazh-studios/widgetbridge/tree/main/widgetbridge-test/src")
            remoteLineSuffix.set("#L")
        }
    }
    pluginsConfiguration.html {
        footerMessage.set("WidgetBridge, Apache 2.0, Vaazh Studios")
    }
}
