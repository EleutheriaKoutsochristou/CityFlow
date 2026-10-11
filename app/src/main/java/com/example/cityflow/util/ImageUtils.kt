package com.example.cityflow.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Utility helper for image manipulation, compression, and format encoding.
 * Converts captured photos or selected gallery images into compact Base64 strings
 * for direct document storage within Cloud Firestore while preventing memory spikes.
 */
object ImageUtils {

    /**
     * Resizes and compresses a [Bitmap] into a compact Base64 JPEG string.
     *
     * @param bitmap Source bitmap to compress and encode.
     * @param maxDimension Maximum bounding width or height in pixels (default 800px).
     * @param quality JPEG compression quality factor between 0 and 100 (default 75%).
     * @return Base64-encoded string without line wrapping.
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        maxDimension: Int = 800,
        quality: Int = 75
    ): String {
        val scaledBitmap = scaleDownBitmap(bitmap, maxDimension)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 string back into an Android [Bitmap].
     *
     * @param base64Str Base64 encoded image string.
     * @return Decoded [Bitmap] or null if decoding fails.
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads and decodes a [Bitmap] from a given Android content [Uri] (e.g. from PhotoPicker or MediaStore).
     *
     * @param context Application or Activity context.
     * @param uri Target content URI.
     * @return Decoded [Bitmap] or null if reading fails.
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Proportionally scales down a bitmap such that its largest dimension does not exceed [maxDimension].
     *
     * @param realImage Original bitmap.
     * @param maxDimension Maximum allowed dimension.
     * @return Proportionally scaled [Bitmap] or original if already within limits.
     */
    private fun scaleDownBitmap(realImage: Bitmap, maxDimension: Int): Bitmap {
        val width = realImage.width
        val height = realImage.height

        if (width <= maxDimension && height <= maxDimension) {
            return realImage
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (ratio > 1.0f) {
            // Landscape orientation
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            // Portrait or square orientation
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(realImage, targetWidth, targetHeight, true)
    }
}
