package com.live.life.intoxication.dreams

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageUtils {

    suspend fun saveImageToGallery(context: Context, drawableRes: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drawable = ContextCompat.getDrawable(context, drawableRes)
                val bitmap = (drawable as BitmapDrawable).bitmap

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageToGalleryQ(context, bitmap, "emoji_${System.currentTimeMillis()}.png")
                } else {
                    saveImageToGalleryLegacy(context, bitmap, "emoji_${System.currentTimeMillis()}.png")
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun saveImageToGalleryQ(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EmojiMixer")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        return uri
    }

    private fun saveImageToGalleryLegacy(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val imagesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "EmojiMixer")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val imageFile = File(imagesDir, fileName)
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    suspend fun shareImage(context: Context, drawableRes: Int) {
        withContext(Dispatchers.IO) {
            val bitmap = ContextCompat.getDrawable(context, drawableRes)?.toBitmap()
            bitmap?.let { bitmap ->
                    try {
                        val fileName = "ems_${System.currentTimeMillis()}.png"
                        val cacheDir = File(context.cacheDir, "shared_images")
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs()
                        }

                        val file = File(cacheDir, fileName)
                        val fos = FileOutputStream(file)

                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                        fos.close()

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, "shared images")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooser = Intent.createChooser(shareIntent, "Share the QR code")
                        context.startActivity(chooser)

                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Sharing failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Toast.makeText(context, "There is no picture to share", Toast.LENGTH_SHORT).show()
                }

        }
    }



    suspend fun batchSaveImages(context: Context, imageList: List<Int>): Int {
        return withContext(Dispatchers.IO) {
            var successCount = 0
            for (imageRes in imageList) {
                if (saveImageToGallery(context, imageRes)) {
                    successCount++
                }
            }
            successCount
        }
    }
}