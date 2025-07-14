package com.live.life.intoxication.dreams

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object BitmapComposer {

    private val resourceCache = object : android.util.LruCache<Int, Bitmap>(50) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun createCompositeBitmapAsync(
        context: Context,
        data: CompositeImageData,
        targetSize: Int = 96
    ): Bitmap? = withContext(Dispatchers.IO) {
        OptimizedBitmapCache.getBitmapAsync(context, data, targetSize)
    }


    fun createCompositeBitmapSync(
        context: Context,
        data: CompositeImageData,
        targetSize: Int = 96
    ): Bitmap? {
        val cacheKey = OptimizedBitmapCache.generateKey(data)
        OptimizedBitmapCache.getBitmap(cacheKey)?.let { return it }

        return try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val faceBitmap = getBitmapFromResourceOptimized(context, data.faceResId, targetSize)
            val eyesBitmap = getBitmapFromResourceOptimized(context, data.eyeResId, targetSize)
            val mouthBitmap = getBitmapFromResourceOptimized(context, data.mouthResId, targetSize)

            faceBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            eyesBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            mouthBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

            OptimizedBitmapCache.putBitmap(cacheKey, resultBitmap)

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun getBitmapFromResourceOptimized(
        context: Context,
        resId: Int,
        targetSize: Int
    ): Bitmap? {
        resourceCache.get(resId)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) {
                return if (cachedBitmap.width == targetSize && cachedBitmap.height == targetSize) {
                    cachedBitmap
                } else {
                    Bitmap.createScaledBitmap(cachedBitmap, targetSize, targetSize, true)
                }
            } else {
                resourceCache.remove(resId)
            }
        }

        return try {
            val drawable = ContextCompat.getDrawable(context, resId)
            val bitmap = if (drawable is BitmapDrawable) {
                val originalBitmap = drawable.bitmap
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true)

                if (!originalBitmap.isRecycled) {
                    resourceCache.put(resId, originalBitmap)
                }

                scaledBitmap
            } else {
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, targetSize, targetSize)
                drawable?.draw(canvas)
                bitmap
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    suspend fun preloadCompositeImages(context: Context, dataList: List<CompositeImageData>) {
        withContext(Dispatchers.IO) {
            dataList.chunked(10).forEach { chunk ->
                chunk.map { data ->
                    async {
                        OptimizedBitmapCache.preloadBitmap(context, data)
                    }
                }.awaitAll()
            }
        }
    }

    fun createCompositeBitmap(
        context: Context,
        data: CompositeImageData,
        targetSize: Int = 96
    ): Bitmap? {
        return createCompositeBitmapSync(context, data, targetSize)
    }
}