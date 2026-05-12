package com.example.circlebirthdays

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun compressImage(context: Context, uri: Uri, quality: Int = 80, maxWidth: Int = 1024, maxHeight: Int = 1024): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var scale = 1
            while (options.outWidth / scale / 2 >= maxWidth && options.outHeight / scale / 2 >= maxHeight) {
                scale *= 2
            }

            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val secondStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(secondStream, null, finalOptions)
            secondStream?.close()

            if (bitmap == null) return null

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
