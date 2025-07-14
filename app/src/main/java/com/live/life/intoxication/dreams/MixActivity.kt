package com.live.life.intoxication.dreams

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.live.life.intoxication.dreams.databinding.ActivityMixBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MixActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMixBinding
    private var currentSelectedTab = -1
    private val checkImageViews = mutableListOf<android.widget.ImageView>()

    private lateinit var emojiAdapter: OptimizedEmojiAdapter
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 存储当前选中的图片数据，用于合成
    private var result1ItemData: EmojiItemData? = null
    private var result2ItemData: EmojiItemData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMixBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mix)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()

        onBackPressedDispatcher.addCallback {
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        startPreloadingCompositeImages()

        selectTab(0)
    }
    private fun startPreloadingCompositeImages() {
        preloadScope.launch {
            try {
                val compositeDataList = CompositeImageDataGenerator.generateCompositeImageList()
                BitmapComposer.preloadCompositeImages(this@MixActivity, compositeDataList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun initViews() {
        checkImageViews.addAll(listOf(
            binding.imgCheck1,
            binding.imgCheck2,
            binding.imgCheck3,
            binding.imgCheck4,
            binding.imgCheck5,
            binding.imgCheck6
        ))
    }

    private fun setupRecyclerView() {
        emojiAdapter = OptimizedEmojiAdapter { itemData, position ->
            onImageItemClick(itemData, position)
        }

        binding.rvEmo.apply {
            layoutManager = GridLayoutManager(this@MixActivity, 4)
            adapter = emojiAdapter

            setHasFixedSize(true)
            setItemViewCacheSize(32)

            recycledViewPool.setMaxRecycledViews(OptimizedEmojiAdapter.TYPE_SINGLE, 16)
            recycledViewPool.setMaxRecycledViews(OptimizedEmojiAdapter.TYPE_COMPOSITE, 16)

            layoutManager?.isItemPrefetchEnabled = true

            isNestedScrollingEnabled = false

            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        preloadVisibleItems()
                    }
                }
            })
        }
    }
    private fun preloadVisibleItems() {
        val layoutManager = binding.rvEmo.layoutManager as? GridLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return
        }

        preloadScope.launch {
            try {
                val currentList = getCurrentItemList()
                val preloadRange = (firstVisible..lastVisible.coerceAtMost(currentList.size - 1))

                for (position in preloadRange) {
                    val item = currentList.getOrNull(position)
                    if (item is EmojiItemData.CompositeImage) {
                        val cacheKey = OptimizedBitmapCache.generateKey(item.data)
                        if (OptimizedBitmapCache.getBitmap(cacheKey) == null) {
                            OptimizedBitmapCache.preloadBitmap(this@MixActivity, item.data, 48)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateListData() {
        val itemList = when (currentSelectedTab) {
            0 -> {
                CompositeImageDataGenerator.generateCompositeImageList()
                    .map { EmojiItemData.CompositeImage(it) }
            }
            1 -> ImageViewData.imageMask.map { EmojiItemData.SingleImage(it) }
            2 -> ImageViewData.imageHat.map { EmojiItemData.SingleImage(it) }
            3 -> ImageViewData.imageBeard.map { EmojiItemData.SingleImage(it) }
            4 -> ImageViewData.imageGlasses.map { EmojiItemData.SingleImage(it) }
            5 -> ImageViewData.imageHands.map { EmojiItemData.SingleImage(it) }
            else -> emptyList<EmojiItemData>()
        }

        emojiAdapter.updateData(itemList)
        binding.rvEmo.scrollToPosition(0)

        preloadScope.launch {
            delay(100)
            preloadVisibleItems()
        }
    }

    private fun setupClickListeners() {
        binding.imageViewBack.setOnClickListener { finish() }
        binding.llEmoFace.setOnClickListener { selectTab(0) }
        binding.llEmoMask.setOnClickListener { selectTab(1) }
        binding.llEmoHat.setOnClickListener { selectTab(2) }
        binding.llEmoBeard.setOnClickListener { selectTab(3) }
        binding.llEmoGlasses.setOnClickListener { selectTab(4) }
        binding.llEmoHands.setOnClickListener { selectTab(5) }

        binding.flRef.setOnClickListener {
            clearResult1()
        }
    }

    private fun selectTab(tabIndex: Int) {
        if (currentSelectedTab == tabIndex) return

        currentSelectedTab = tabIndex
        updateTabSelection()
        updateListData()
    }

    private fun updateTabSelection() {
        checkImageViews.forEach { it.visibility = View.GONE }

        if (currentSelectedTab < checkImageViews.size) {
            checkImageViews[currentSelectedTab].visibility = View.VISIBLE
        }
    }



    private fun onImageItemClick(itemData: EmojiItemData, position: Int) {
        if (result1ItemData == null) {
            setResultImage(binding.imgResult1, itemData)
            binding.imgResult1.visibility = View.VISIBLE
            binding.tv1.visibility = View.GONE  // 隐藏tv_1
            result1ItemData = itemData
        } else if (result2ItemData == null) {
            setResultImage(binding.imgResult2, itemData)
            binding.imgResult2.visibility = View.VISIBLE
            binding.tv2.visibility = View.GONE
            result2ItemData = itemData

            mergeImagesAndNavigate()
        }
    }



    private fun clearResult1() {
        binding.imgResult1.visibility = View.GONE
        binding.tv1.visibility = View.VISIBLE
        result1ItemData = null

        if (result2ItemData != null) {
            result1ItemData = result2ItemData
            result2ItemData = null

            setResultImage(binding.imgResult1, result1ItemData!!)
            binding.imgResult1.visibility = View.VISIBLE
            binding.tv1.visibility = View.GONE

            binding.imgResult2.visibility = View.GONE
            binding.tv2.visibility = View.VISIBLE
        }
    }

    private fun mergeImagesAndNavigate() {
        if (result1ItemData == null || result2ItemData == null) {
            return
        }

        try {
            val mergedBitmap = createMergedBitmap()

            if (mergedBitmap != null) {
                MergedImageHolder.mergedBitmap = mergedBitmap

                val intent = Intent(this, ResultActivity::class.java)
                startActivity(intent)

                resetAfterMerge()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun createMergedBitmap(): Bitmap? {
        try {
            val targetSize = 96

            val item1 = result1ItemData!!
            val item2 = result2ItemData!!

            val isBothFace = item1 is EmojiItemData.CompositeImage && item2 is EmojiItemData.CompositeImage

            return if (isBothFace) {
                createRandomFaceMix(item1 as EmojiItemData.CompositeImage, item2 as EmojiItemData.CompositeImage, targetSize)
            } else {
                createSimpleOverlay(item1, item2, targetSize)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    private fun createRandomFaceMix(
        compositeData1: EmojiItemData.CompositeImage,
        compositeData2: EmojiItemData.CompositeImage,
        targetSize: Int
    ): Bitmap? {
        try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val availableFaces = listOf(compositeData1.data.faceResId, compositeData2.data.faceResId)
            val availableEyes = listOf(compositeData1.data.eyeResId, compositeData2.data.eyeResId)
            val availableMouths = listOf(compositeData1.data.mouthResId, compositeData2.data.mouthResId)

            val allFaces = ImageViewData.imageFace
            val allEyes = ImageViewData.imageEye
            val allMouths = ImageViewData.imageMouth

            val selectedFace = if (Random.nextBoolean()) {
                availableFaces.random()
            } else {
                allFaces.random()
            }

            val selectedEyes = if (Random.nextBoolean()) {
                availableEyes.random()
            } else {
                allEyes.random()
            }

            val selectedMouth = if (Random.nextBoolean()) {
                availableMouths.random()
            } else {
                allMouths.random()
            }

            val newCompositeData = CompositeImageData(selectedFace, selectedEyes, selectedMouth)

            return BitmapComposer.createCompositeBitmap(this, newCompositeData, targetSize)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }



    private fun createSimpleOverlay(
        itemData1: EmojiItemData,
        itemData2: EmojiItemData,
        targetSize: Int
    ): Bitmap? {
        try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            val bitmap1 = getBitmapFromItemData(itemData1, targetSize)
            val bitmap2 = getBitmapFromItemData(itemData2, targetSize)

            bitmap1?.let {
                if (!it.isRecycled) {
                    val scaledBitmap1 = if (it.width != targetSize || it.height != targetSize) {
                        Bitmap.createScaledBitmap(it, targetSize, targetSize, true)
                    } else {
                        it
                    }
                    canvas.drawBitmap(scaledBitmap1, 0f, 0f, null)

                    if (scaledBitmap1 != it && !scaledBitmap1.isRecycled) {
                        scaledBitmap1.recycle()
                    }
                }
            }

            bitmap2?.let {
                if (!it.isRecycled) {
                    val scaledBitmap2 = if (it.width != targetSize || it.height != targetSize) {
                        Bitmap.createScaledBitmap(it, targetSize, targetSize, true)
                    } else {
                        it
                    }

                    val paint = android.graphics.Paint().apply {
                        alpha = 180
                        isAntiAlias = true
                    }
                    canvas.drawBitmap(scaledBitmap2, 0f, 0f, paint)

                    if (scaledBitmap2 != it && !scaledBitmap2.isRecycled) {
                        scaledBitmap2.recycle()
                    }
                }
            }

            return resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    private fun getBitmapFromItemData(itemData: EmojiItemData, targetSize: Int): Bitmap? {
        return when (itemData) {
            is EmojiItemData.SingleImage -> {
                getBitmapFromResource(itemData.imageResId, targetSize)
            }
            is EmojiItemData.CompositeImage -> {
                BitmapComposer.createCompositeBitmap(this, itemData.data, targetSize)
            }
        }
    }


    private fun getBitmapFromResource(resId: Int, targetSize: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(this, resId)
            if (drawable is BitmapDrawable) {
                val originalBitmap = drawable.bitmap
                Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true)
            } else {
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable?.setBounds(0, 0, targetSize, targetSize)  // 设置边界为整个画布
                drawable?.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun drawableToBitmap(drawable: Drawable, targetSize: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bitmap
    }


    private fun setResultImage(imageView: android.widget.ImageView, itemData: EmojiItemData) {
        when (itemData) {
            is EmojiItemData.SingleImage -> {
                imageView.setImageResource(itemData.imageResId)
                imageView.scaleType = ImageView.ScaleType.FIT_XY
            }
            is EmojiItemData.CompositeImage -> {
                val cacheKey = OptimizedBitmapCache.generateKey(itemData.data)
                val cachedBitmap = OptimizedBitmapCache.getBitmap(cacheKey)

                if (cachedBitmap != null) {
                    imageView.setImageBitmap(cachedBitmap)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY
                } else {
                    imageView.setImageResource(itemData.data.faceResId)
                    imageView.scaleType = ImageView.ScaleType.FIT_XY

                    lifecycleScope.launch {
                        try {
                            val bitmap = BitmapComposer.createCompositeBitmapAsync(
                                this@MixActivity,
                                itemData.data,
                                56
                            )
                            bitmap?.let {
                                imageView.setImageBitmap(it)
                                imageView.scaleType = ImageView.ScaleType.FIT_XY
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
    private fun resetAfterMerge() {
        result1ItemData = null
        result2ItemData = null
        binding.imgResult1.visibility = View.GONE
        binding.imgResult2.visibility = View.GONE
        binding.tv1.visibility = View.VISIBLE
        binding.tv2.visibility = View.VISIBLE
    }


    fun getCurrentItemList(): List<EmojiItemData> {
        return when (currentSelectedTab) {
            0 -> CompositeImageDataGenerator.generateCompositeImageList()
                .map { EmojiItemData.CompositeImage(it) }
            1 -> ImageViewData.imageMask.map { EmojiItemData.SingleImage(it) }
            2 -> ImageViewData.imageHat.map { EmojiItemData.SingleImage(it) }
            3 -> ImageViewData.imageBeard.map { EmojiItemData.SingleImage(it) }
            4 -> ImageViewData.imageGlasses.map { EmojiItemData.SingleImage(it) }
            5 -> ImageViewData.imageHands.map { EmojiItemData.SingleImage(it) }
            else -> emptyList()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        emojiAdapter.cleanup()
        preloadScope.cancel()

    }
}

object MergedImageHolder {
    var mergedBitmap: Bitmap? = null
}