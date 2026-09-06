package com.vocabloot.widgetbridge.sample

/**
 * The six cover images bundled with the sample (Android `assets/covers`, iOS app bundle `Covers`).
 * A real app already has image files on disk (photos, downloads); the sample copies its bundled
 * ones into [cacheDirectory] once so that `WidgetImages.encode` has a file path to read.
 */
object SampleImages {
    const val COUNT = 6

    fun name(index: Int): String = "q${index + 1}.jpg"

    /** Returns the on-disk path for each bundled image, copying them out of the bundle on first use. */
    fun materialize(directory: String): List<String> {
        val folder = "$directory/widgetbridge-sample"
        ensureDirectory(folder)
        return (0 until COUNT).map { index ->
            val target = "$folder/${name(index)}"
            if (!fileExists(target)) writeFile(target, readBundledCover(name(index)))
            target
        }
    }
}
