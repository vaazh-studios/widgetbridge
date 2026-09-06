package com.vocabloot.widgetbridge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

public actual object WidgetImages {
    private const val MIN_DIMENSION = 128

    public actual fun encode(sourcePath: String, format: WidgetImageFormat, maxDimension: Int, maxBytes: Int): EncodedImage? {
        if (maxBytes <= 0 || !File(sourcePath).isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourcePath, bounds)
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (longSide <= 0) return null
        var sample = 1
        while (longSide / sample > maxDimension * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(sourcePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val oriented = if (format == WidgetImageFormat.Jpeg) applyExif(decoded, sourcePath) else decoded
        try {
            var dimension = maxDimension
            while (dimension >= MIN_DIMENSION) {
                val candidate = scaleToFit(oriented, dimension)
                val bytes = candidate.encode(format)
                if (candidate !== oriented) candidate.recycle()
                if (bytes.size <= maxBytes) return EncodedImage(bytes, format.extension)
                dimension = (dimension * 3 / 4).coerceAtLeast(MIN_DIMENSION - 1)
            }
            return null
        } finally {
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    private fun applyExif(bitmap: Bitmap, path: String): Bitmap {
        val orientation = runCatching { ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToFit(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longSide
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun Bitmap.encode(format: WidgetImageFormat): ByteArray = ByteArrayOutputStream().use { out ->
        if (format == WidgetImageFormat.Png) compress(Bitmap.CompressFormat.PNG, 100, out) else compress(Bitmap.CompressFormat.JPEG, 80, out)
        out.toByteArray()
    }
}
