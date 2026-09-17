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
 * Βοηθητική κλάση για επεξεργασία και συμπίεση εικόνων.
 * Μετατρέπει εικόνες σε Base64 για άμεση αποθήκευση στο Firestore
 * με αυτόματο resize ώστε να μην επιβαρύνεται η μνήμη ή το δίκτυο.
 */
object ImageUtils {

    /**
     * Μετατροπή Bitmap σε Base64 string με μέγιστη διάσταση και συμπίεση JPEG.
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
     * Μετατροπή Base64 string σε Bitmap.
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
     * Ανάκτηση Bitmap από Uri (π.χ. από Photo Picker / Gallery).
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
     * Αναλογική σμίκρυνση διαστάσεων του Bitmap.
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
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(realImage, targetWidth, targetHeight, true)
    }
}
