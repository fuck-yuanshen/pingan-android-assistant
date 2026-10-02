package com.example.pingan.logic.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decodes user-selected images with APIs available on the app's minimum Android version.
 */
fun decodeImageBitmapFromUri(
    context: Context,
    imageUri: String,
    maxSide: Int? = null,
): ImageBitmap? {
    if (imageUri.isBlank()) return null

    return runCatching {
        val uri = Uri.parse(imageUri)
        val sampleSize = resolveSampleSize(context, uri, maxSide)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun resolveSampleSize(context: Context, uri: Uri, maxSide: Int?): Int {
    if (maxSide == null || maxSide <= 0) return 1

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bounds)
        }
    }

    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return 1

    var sampleSize = 1
    while ((width / sampleSize) > maxSide || (height / sampleSize) > maxSide) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}
