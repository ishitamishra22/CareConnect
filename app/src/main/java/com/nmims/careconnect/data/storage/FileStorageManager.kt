package com.nmims.careconnect.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages low-level Android internal file storage operations.
 * Runs strictly on Dispatchers.IO with comprehensive try/catch blocks.
 * Zero external database or networking libraries.
 */
class FileStorageManager(private val context: Context) {

    private val documentsDir: File by lazy {
        val dir = File(context.filesDir, "documents")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }


    suspend fun saveBitmap(bitmap: Bitmap, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(documentsDir, fileName)
            FileOutputStream(targetFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveImageFromUri(uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(documentsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext null
            targetFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun loadBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null

        return try {
            // First decode with inJustDecodeBounds = true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Calculate sample size so large camera images do not cause OOM
            var inSampleSize = 1
            val reqWidth = 1024
            val reqHeight = 1024
            val (height: Int, width: Int) = options.outHeight to options.outWidth

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cacheFile(documentId: String): File {
        return File(documentsDir, "$documentId.jpg")
    }

    suspend fun compressToJpegBytes(bitmap: Bitmap): ByteArray = withContext(Dispatchers.IO) {
        val width = bitmap.width
        val height = bitmap.height
        val maxDim = maxOf(width, height)
        val scaled = if (maxDim > 1280) {
            val scale = 1280f / maxDim
            val targetWidth = (width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
        val stream = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.toByteArray()
    }

    suspend fun bytesFromUri(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            compressToJpegBytes(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
