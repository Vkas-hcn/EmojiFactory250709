package com.live.life.intoxication.dreams

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.live.life.intoxication.dreams.databinding.ActivityMixBinding
import kotlin.random.Random

class MixActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMixBinding
    private var currentSelectedTab = -1
    private val checkImageViews = mutableListOf<android.widget.ImageView>()

    private lateinit var emojiAdapter: EnhancedEmojiAdapter

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

        // 默认选中第一个选项卡
        selectTab(0)
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

// 在MixActivity.kt中的setupRecyclerView()方法需要这样优化：

    private fun setupRecyclerView() {
        emojiAdapter = EnhancedEmojiAdapter { itemData, position ->
            onImageItemClick(itemData, position)
        }

        binding.rvEmo.apply {
            layoutManager = GridLayoutManager(this@MixActivity, 4)
            adapter = emojiAdapter

            // 性能优化设置
            setHasFixedSize(true)  // 保持原有
            setItemViewCacheSize(20)  // 保持原有

            // 新增优化设置
            recycledViewPool.setMaxRecycledViews(EnhancedEmojiAdapter.TYPE_SINGLE, 10)
            recycledViewPool.setMaxRecycledViews(EnhancedEmojiAdapter.TYPE_COMPOSITE, 10)

            // 预取优化
            layoutManager?.isItemPrefetchEnabled = true

            // 禁用嵌套滑动（如果不需要的话）
            isNestedScrollingEnabled = false

            // 禁用动画（可选，如果不需要item动画的话）
            // itemAnimator = null
        }
    }

    // updateListData()方法保持不变，DiffUtil会自动处理差异更新
    private fun updateListData() {
        val itemList = when (currentSelectedTab) {
            0 -> {
                // Face类别使用复合图像
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
        // 滑动到顶部（如果需要的话）
        binding.rvEmo.scrollToPosition(0)
    }

    private fun setupClickListeners() {
        binding.imageViewBack.setOnClickListener { finish() }
        binding.llEmoFace.setOnClickListener { selectTab(0) }
        binding.llEmoMask.setOnClickListener { selectTab(1) }
        binding.llEmoHat.setOnClickListener { selectTab(2) }
        binding.llEmoBeard.setOnClickListener { selectTab(3) }
        binding.llEmoGlasses.setOnClickListener { selectTab(4) }
        binding.llEmoHands.setOnClickListener { selectTab(5) }

        // 添加清除按钮点击事件
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
        // 优先显示在img_result_1，只有img_result_1有图片后才能显示到img_result_2
        if (result1ItemData == null) {
            // 如果img_result_1没有图片，优先显示在img_result_1
            setResultImage(binding.imgResult1, itemData)
            binding.imgResult1.visibility = View.VISIBLE
            binding.tv1.visibility = View.GONE  // 隐藏tv_1
            result1ItemData = itemData
        } else if (result2ItemData == null) {
            // 如果img_result_1已有图片，且img_result_2没有图片，显示在img_result_2
            setResultImage(binding.imgResult2, itemData)
            binding.imgResult2.visibility = View.VISIBLE
            binding.tv2.visibility = View.GONE  // 隐藏tv_2
            result2ItemData = itemData

            // 自动合成并跳转
            mergeImagesAndNavigate()
        }
        // 如果两个位置都有图片，不做任何操作（或者可以替换第二个）
    }



    private fun clearResult1() {
        // 清除img_result_1的图片并显示tv_1
        binding.imgResult1.visibility = View.GONE
        binding.tv1.visibility = View.VISIBLE
        result1ItemData = null

        // 如果result2有图片，将其移动到result1
        if (result2ItemData != null) {
            result1ItemData = result2ItemData
            result2ItemData = null

            // 将result2的图片显示到result1
            setResultImage(binding.imgResult1, result1ItemData!!)
            binding.imgResult1.visibility = View.VISIBLE
            binding.tv1.visibility = View.GONE

            // 清除result2
            binding.imgResult2.visibility = View.GONE
            binding.tv2.visibility = View.VISIBLE
        }
    }

    private fun mergeImagesAndNavigate() {
        // 确保两个图片都已选择
        if (result1ItemData == null || result2ItemData == null) {
            return
        }

        try {
            // 创建合成图片
            val mergedBitmap = createMergedBitmap()

            if (mergedBitmap != null) {
                // 将合成的图片保存到静态变量或通过Intent传递
                // 这里我使用Application类或静态变量的方式
                MergedImageHolder.mergedBitmap = mergedBitmap

                // 跳转到ResultActivity
                val intent = Intent(this, ResultActivity::class.java)
                startActivity(intent)

                // 可选：重置当前状态
                resetAfterMerge()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理错误，可以显示Toast等
        }
    }

    /**
     * 创建合成图片的主要方法
     * 支持两种合成模式：
     * 1. Face + Face = 随机重新组合脸部元素
     * 2. Face + Other = 简单叠加
     */
    private fun createMergedBitmap(): Bitmap? {
        try {
            val targetSize = 96

            val item1 = result1ItemData!!
            val item2 = result2ItemData!!

            // 判断合成类型
            val isBothFace = item1 is EmojiItemData.CompositeImage && item2 is EmojiItemData.CompositeImage

            return if (isBothFace) {
                // Face + Face：随机重新组合脸部元素
                createRandomFaceMix(item1 as EmojiItemData.CompositeImage, item2 as EmojiItemData.CompositeImage, targetSize)
            } else {
                // Face + Other 或 Other + Other：简单叠加
                createSimpleOverlay(item1, item2, targetSize)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 创建随机脸部混合（Face + Face）
     * 从两个复合图像中随机选择脸、眼睛、嘴巴的组合
     */
    private fun createRandomFaceMix(
        compositeData1: EmojiItemData.CompositeImage,
        compositeData2: EmojiItemData.CompositeImage,
        targetSize: Int
    ): Bitmap? {
        try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 从两个复合数据中收集所有可用的元素
            val availableFaces = listOf(compositeData1.data.faceResId, compositeData2.data.faceResId)
            val availableEyes = listOf(compositeData1.data.eyeResId, compositeData2.data.eyeResId)
            val availableMouths = listOf(compositeData1.data.mouthResId, compositeData2.data.mouthResId)

            // 也可以从全局资源池中随机选择，增加更多变化
            val allFaces = ImageViewData.imageFace
            val allEyes = ImageViewData.imageEye
            val allMouths = ImageViewData.imageMouth

            // 随机选择元素（50%概率从当前两个选择，50%概率从全局随机选择）
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

            // 创建新的复合数据并合成
            val newCompositeData = CompositeImageData(selectedFace, selectedEyes, selectedMouth)

            // 使用BitmapComposer创建最终图像
            return BitmapComposer.createCompositeBitmap(this, newCompositeData, targetSize)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 创建简单叠加合成（Face + Other 或 Other + Other）
     * 修复版本：不会过早回收bitmap
     */
    private fun createSimpleOverlay(
        itemData1: EmojiItemData,
        itemData2: EmojiItemData,
        targetSize: Int
    ): Bitmap? {
        try {
            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 获取第一个图片的Bitmap
            val bitmap1 = getBitmapFromItemData(itemData1, targetSize)
            // 获取第二个图片的Bitmap
            val bitmap2 = getBitmapFromItemData(itemData2, targetSize)

            // 绘制第一个图片
            bitmap1?.let {
                if (!it.isRecycled) {
                    canvas.drawBitmap(it, 0f, 0f, null)
                }
            }

            // 绘制第二个图片（叠加效果）
            bitmap2?.let {
                if (!it.isRecycled) {
                    val paint = android.graphics.Paint().apply {
                        alpha = 180 // 设置透明度，可以调整
                    }
                    canvas.drawBitmap(it, 0f, 0f, paint)
                }
            }

            // 重要：不要在这里回收bitmap1和bitmap2
            // 因为它们可能还在其他地方被使用（如ImageView中）
            // 让GC自动处理内存回收

            return resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getBitmapFromItemData(itemData: EmojiItemData, targetSize: Int): Bitmap? {
        return when (itemData) {
            is EmojiItemData.SingleImage -> {
                // 从资源创建Bitmap
                val drawable = ContextCompat.getDrawable(this, itemData.imageResId)
                drawable?.let { drawableToBitmap(it, targetSize) }
            }
            is EmojiItemData.CompositeImage -> {
                // 使用BitmapComposer创建复合图片
                BitmapComposer.createCompositeBitmap(this, itemData.data, targetSize)
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable, targetSize: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * 优化的setResultImage方法，避免bitmap回收问题
     */
    private fun setResultImage(imageView: android.widget.ImageView, itemData: EmojiItemData) {
        when (itemData) {
            is EmojiItemData.SingleImage -> {
                // 直接使用资源ID，避免bitmap回收问题
                imageView.setImageResource(itemData.imageResId)
            }
            is EmojiItemData.CompositeImage -> {
                // 显示完整的复合图像
                val compositeBitmap = BitmapComposer.createCompositeBitmap(
                    this,
                    itemData.data,
                    targetSize = 56 // 根据你的ImageView大小调整
                )
                if (compositeBitmap != null && !compositeBitmap.isRecycled) {
                    imageView.setImageBitmap(compositeBitmap)
                } else {
                    // 如果合成失败，显示face部分
                    imageView.setImageResource(itemData.data.faceResId)
                }
            }
        }
    }

    private fun resetAfterMerge() {
        // 可选：重置所有状态
        result1ItemData = null
        result2ItemData = null
        binding.imgResult1.visibility = View.GONE
        binding.imgResult2.visibility = View.GONE
        binding.tv1.visibility = View.VISIBLE
        binding.tv2.visibility = View.VISIBLE
    }

    // 获取当前选中的选项卡
    fun getCurrentSelectedTab(): Int = currentSelectedTab

    // 获取当前显示的数据列表
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

    // 重置状态
    fun resetState() {
        binding.imgResult1.visibility = View.GONE
        binding.imgResult2.visibility = View.GONE
        binding.tv1.visibility = View.VISIBLE
        binding.tv2.visibility = View.VISIBLE
        result1ItemData = null
        result2ItemData = null
    }
}

// 用于在Activity之间传递合成图片的辅助类
object MergedImageHolder {
    var mergedBitmap: Bitmap? = null
}