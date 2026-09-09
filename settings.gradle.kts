rootProject.name = "widgetbridge"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":widgetbridge")
include(":widgetbridge-test")
include(":sample:shared")
include(":sample:androidApp")
