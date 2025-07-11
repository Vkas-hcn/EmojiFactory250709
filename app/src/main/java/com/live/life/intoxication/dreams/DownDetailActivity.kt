package com.live.life.intoxication.dreams

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.live.life.intoxication.dreams.databinding.ActivityDownDetailBinding
import kotlinx.coroutines.launch

class DownDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownDetailBinding
    private var currentImageList: List<Int> = emptyList()
    private var currentImageRes: Int = 0
    private var imageType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDownDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()

        onBackPressedDispatcher.addCallback {
            finish()
        }

        setupData()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupData() {
        imageType = intent.getStringExtra(MainActivity.EXTRA_IMAGE_TYPE) ?: ""

        currentImageList = when (imageType) {
            MainActivity.TYPE_FRUITS -> ImageViewData.downloadImageFruits
            MainActivity.TYPE_SMILE -> ImageViewData.downloadImageSmile
            MainActivity.TYPE_ALIEN -> ImageViewData.downloadImageAlien
            else -> emptyList()
        }

        // 设置标题
        binding.textView4.text = when (imageType) {
            MainActivity.TYPE_FRUITS -> "Fruits"
            MainActivity.TYPE_SMILE -> "Smile"
            MainActivity.TYPE_ALIEN -> "Alien"
            else -> "Images"
        }
    }

    private fun setupRecyclerView() {
        binding.rvDownList.apply {
            layoutManager = GridLayoutManager(this@DownDetailActivity, 4)
            adapter = GridImageAdapter(currentImageList) { position, imageRes ->
                showImageDialog(imageRes)
            }
        }
    }

    private fun setupClickListeners() {
        // 返回按钮
        binding.imgBack.setOnClickListener {
            finish()
        }

        // 批量下载按钮
        binding.imgDown.setOnClickListener {
            // Android 10+ 不需要权限检查，直接下载
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                batchDownloadImages()
            } else {
                // Android 9 及以下需要权限
                if (PermissionManager.hasStoragePermission(this)) {
                    batchDownloadImages()
                } else {
                    PermissionManager.requestStoragePermission(this)
                }
            }
        }

        // 对话框关闭按钮
        binding.imgClosure.setOnClickListener {
            hideImageDialog()
        }

        // 分享按钮
        binding.iconShare.setOnClickListener {
            lifecycleScope.launch {
                ImageUtils.shareImage(this@DownDetailActivity, currentImageRes)
            }
        }

        // 下载单张图片按钮
        binding.iconDownload.setOnClickListener {
            // Android 10+ 不需要权限检查，直接下载
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadSingleImage()
            } else {
                // Android 9 及以下需要权限
                if (PermissionManager.hasStoragePermission(this)) {
                    downloadSingleImage()
                } else {
                    PermissionManager.requestStoragePermission(this)
                }
            }
        }

        // 点击对话框背景关闭
        binding.conDialog.setOnClickListener {
            hideImageDialog()
        }

        // 防止点击内容区域关闭对话框
        binding.linearLayout.setOnClickListener {
            // 不做任何操作
        }
    }

    private fun showImageDialog(imageRes: Int) {
        currentImageRes = imageRes
        binding.imgCheck.setImageResource(imageRes)
        binding.conDialog.visibility = View.VISIBLE
    }

    private fun hideImageDialog() {
        binding.conDialog.visibility = View.GONE
    }

    private fun batchDownloadImages() {
        lifecycleScope.launch {
            val savedCount = ImageUtils.batchSaveImages(this@DownDetailActivity, currentImageList)
            Toast.makeText(
                this@DownDetailActivity,
                "Saved successfully $savedCount Pictures to photo album",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun downloadSingleImage() {
        lifecycleScope.launch {
            val success = ImageUtils.saveImageToGallery(this@DownDetailActivity, currentImageRes)
            if (success) {
                Toast.makeText(this@DownDetailActivity, "Images saved to album", Toast.LENGTH_SHORT).show()
                hideImageDialog()
            } else {
                Toast.makeText(this@DownDetailActivity, "Saving failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionManager.STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // 权限被授予，可以执行下载操作
                    Toast.makeText(this, "The permission has been obtained, you can download the picture", Toast.LENGTH_SHORT).show()
                } else {
                    // 权限被拒绝
                    if (PermissionManager.shouldShowRequestPermissionRationale(this)) {
                        // 用户选择了"不再询问"
                        PermissionManager.showPermissionDeniedDialog(this)
                    } else {
                        // 用户拒绝了权限
                        Toast.makeText(this, "Need to save pictures", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}