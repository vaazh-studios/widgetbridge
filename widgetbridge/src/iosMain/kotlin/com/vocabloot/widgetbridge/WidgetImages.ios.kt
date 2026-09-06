package com.vocabloot.widgetbridge

import com.vocabloot.widgetbridge.internal.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public actual object WidgetImages {
    private const val MIN_DIMENSION = 128

    public actual fun encode(sourcePath: String, format: WidgetImageFormat, maxDimension: Int, maxBytes: Int): EncodedImage? {
        if (maxBytes <= 0) return null
        val image = UIImage.imageWithContentsOfFile(sourcePath) ?: return null
        var dimension = maxDimension
        while (dimension >= MIN_DIMENSION) {
            val scaled = image.scaledToFit(dimension, opaque = format == WidgetImageFormat.Jpeg) ?: return null
            val data = (if (format == WidgetImageFormat.Png) UIImagePNGRepresentation(scaled) else UIImageJPEGRepresentation(scaled, 0.8)) ?: return null
            val bytes = data.toByteArray()
            if (bytes.size <= maxBytes) return EncodedImage(bytes, format.extension)
            dimension = (dimension * 3 / 4).coerceAtLeast(MIN_DIMENSION - 1)
        }
        return null
    }

    private fun UIImage.scaledToFit(maxDimension: Int, opaque: Boolean): UIImage? {
        val width = size.useContents { width }
        val height = size.useContents { height }
        val longSide = maxOf(width, height)
        if (longSide <= 0.0) return null
        val scale = minOf(1.0, maxDimension.toDouble() / longSide)
        UIGraphicsBeginImageContextWithOptions(size = CGSizeMake(width * scale, height * scale), opaque = opaque, scale = 1.0)
        drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
        val result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result
    }
}
