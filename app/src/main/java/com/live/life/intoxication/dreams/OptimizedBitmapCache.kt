package com.live.life.intoxication.dreams

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.live.life.intoxication.dreams.BitmapComposer.createCompositeBitmapSync
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object OptimizedBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val cache = object : android.util.LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: Bitmap?,
            newValue: Bitmap?
        ) {
            if (evicted && oldValue != null && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    private val preloadingKeys = ConcurrentHashMap<String, Boolean>()

    private val loadingKeys = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    fun getBitmap(key: String): Bitmap? {
        val bitmap = cache.get(key)
        return if (bitmap != null && !bitmap.isRecycled) {
            bitmap
        } else {
            if (bitmap != null) {
                cache.remove(key)
            }
            null
        }
    }

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun generateKey(data: CompositeImageData): String {
        return "${data.faceResId}_${data.eyeResId}_${data.mouthResId}"
    }



    suspend fun preloadBitmap(context: Context, data: CompositeImageData, targetSize: Int = 96) {
        val key = generateKey(data)
        if (getBitmap(key) != null || preloadingKeys.containsKey(key)) {
            return // 已存在或正在预加载
        }

        preloadingKeys[key] = true
        try {
            val bitmap = BitmapComposer.createCompositeBitmapSync(context, data, targetSize)
            bitmap?.let { putBitmap(key, it) }
        } finally {
            preloadingKeys.remove(key)
        }
    }

    suspend fun getBitmapAsync(context: Context, data: CompositeImageData, targetSize: Int = 96): Bitmap? {
        val key = generateKey(data)

        getBitmap(key)?.let { return it }

        loadingKeys[key]?.let { deferred ->
            return try {
                deferred.await()
            } catch (e: Exception) {
                loadingKeys.remove(key)
                null
            }
        }

        val deferred = CoroutineScope(Dispatchers.IO).async {
            try {
                createCompositeBitmapSync(context, data, targetSize)?.also { bitmap ->
                    putBitmap(key, bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        loadingKeys[key] = deferred

        return try {
            val result = deferred.await()
            loadingKeys.remove(key)
            result
        } catch (e: Exception) {
            loadingKeys.remove(key)
            null
        }
    }
}
