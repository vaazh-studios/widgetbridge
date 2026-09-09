# Stability and versions

## Versioning

Semantic versioning from 1.0. Before 1.0, a minor version may change the API; the changelog says so in its first line and the binary API dump in `widgetbridge/api` shows the exact diff. The Swift package follows the same tags.

## The on-disk format

The feed format is part of the contract: a reader of version N reads every generation a writer of version N wrote. Changing it bumps the major version. Your own payload's `schemaVersion` is separate and yours to bump.

## Requirements

| | Minimum | Built and tested with |
|---|---|---|
| Kotlin | 2.3 | 2.3.20 |
| Android Gradle Plugin | 9.0 (`androidLibrary` KMP plugin) | 9.2.1 |
| Gradle | 9.0 | 9.4.1 |
| Android | minSdk 24, compileSdk 36; Glance 1.1 in your app module | Pixel 10 Pro emulator, Android 16 |
| iOS | 16 | iPhone 17 Pro simulator, iOS 26 |
| Xcode / Swift tools | 16 / 5.9 | 26 / 5.9 |
| Coroutines / Serialization | 1.10 / 1.8 | 1.10.2 / 1.8.1 |

## Migration guides

None yet. From the first breaking release on, each major version gets a guide here.
