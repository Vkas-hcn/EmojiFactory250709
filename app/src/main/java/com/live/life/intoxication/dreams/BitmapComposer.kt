package com.live.life.intoxication.dreams

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
object BitmapCache {
    private val cache = object : android.util.LruCache<String, android.graphics.Bitmap>(20) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: android.graphics.Bitmap?,
            newValue: android.graphics.Bitmap?
        ) {
            // 当条目被移除时，回收对应的bitmap
            if (evicted && oldValue != null && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    fun getBitmap(key: String): android.graphics.Bitmap? {
        val bitmap = cache.get(key)
        return if (bitmap != null && !bitmap.isRecycled) {
            bitmap
        } else {
            // 如果bitmap已被回收，从缓存中移除
            if (bitmap != null) {
                cache.remove(key)
            }
            null
        }
    }

    fun putBitmap(key: String, bitmap: android.graphics.Bitmap) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun generateKey(data: CompositeImageData): String {
        return "${data.faceResId}_${data.eyeResId}_${data.mouthResId}"
    }

    fun clearCache() {
        cache.evictAll()
    }

    fun removeBitmap(key: String) {
        cache.remove(key)
    }
}
object BitmapComposer {
    fun createCompositeBitmap(
        context: Context,
        data: CompositeImageData,
        targetSize: Int = 96
    ): Bitmap? {
        val cacheKey = BitmapCache.generateKey(data)
        BitmapCache.getBitmap(cacheKey)?.let { return it }

        return try {
            // 原有的bitmap创建逻辑...
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 加载并绘制face
            val faceBitmap = getBitmapFromResource(context, data.faceResId, targetSize)
            faceBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

            // 加载并绘制eyes
            val eyesBitmap = getBitmapFromResource(context, data.eyeResId, targetSize)
            eyesBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

            // 加载并绘制mouth
            val mouthBitmap = getBitmapFromResource(context, data.mouthResId, targetSize)
            mouthBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

            // 回收临时bitmap
            faceBitmap?.recycle()
            eyesBitmap?.recycle()
            mouthBitmap?.recycle()

            // 缓存结果
            BitmapCache.putBitmap(cacheKey, resultBitmap)

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将复合图像数据合成为单个Bitmap
     */


    /**
     * 从资源ID获取指定大小的Bitmap
     */
    private fun getBitmapFromResource(context: Context, resId: Int, targetSize: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId)
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            } else {
                // 对于其他类型的drawable，先转换为bitmap
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, targetSize, targetSize)
                drawable?.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 创建带透明度的复合图像
     */
    fun createCompositeWithAlpha(
        context: Context,
        data: CompositeImageData,
        targetSize: Int = 96,
        faceAlpha: Float = 1.0f,
        eyesAlpha: Float = 1.0f,
        mouthAlpha: Float = 1.0f
    ): Bitmap? {
        return try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }

            // 绘制face
            val faceBitmap = getBitmapFromResource(context, data.faceResId, targetSize)
            faceBitmap?.let {
                paint.alpha = (255 * faceAlpha).toInt()
                canvas.drawBitmap(it, 0f, 0f, paint)
            }

            // 绘制eyes
            val eyesBitmap = getBitmapFromResource(context, data.eyeResId, targetSize)
            eyesBitmap?.let {
                paint.alpha = (255 * eyesAlpha).toInt()
                canvas.drawBitmap(it, 0f, 0f, paint)
            }

            // 绘制mouth
            val mouthBitmap = getBitmapFromResource(context, data.mouthResId, targetSize)
            mouthBitmap?.let {
                paint.alpha = (255 * mouthAlpha).toInt()
                canvas.drawBitmap(it, 0f, 0f, paint)
            }

            // 回收临时bitmap
            faceBitmap?.recycle()
            eyesBitmap?.recycle()
            mouthBitmap?.recycle()

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}