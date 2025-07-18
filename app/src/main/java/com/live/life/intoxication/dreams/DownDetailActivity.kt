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
            MainActivity.TYPE_SMILE -> ImageViewData.downloadImageEmoji
            MainActivity.TYPE_ALIEN -> ImageViewData.downloadImageAlien
            else -> emptyList()
        }

        binding.textView4.text = when (imageType) {
            MainActivity.TYPE_FRUITS -> "Fruits"
            MainActivity.TYPE_SMILE -> "Emoji"
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
        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.imgDown.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                batchDownloadImages()
            } else {
                if (PermissionManager.hasStoragePermission(this)) {
                    batchDownloadImages()
                } else {
                    PermissionManager.requestStoragePermission(this)
                }
            }
        }

        binding.imgClosure.setOnClickListener {
            hideImageDialog()
        }

        binding.iconShare.setOnClickListener {
            lifecycleScope.launch {
                ImageUtils.shareImage(this@DownDetailActivity, currentImageRes)
            }
        }

        binding.iconDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadSingleImage()
            } else {
                if (PermissionManager.hasStoragePermission(this)) {
                    downloadSingleImage()
                } else {
                    PermissionManager.requestStoragePermission(this)
                }
            }
        }

        binding.conDialog.setOnClickListener {
            hideImageDialog()
        }

        binding.linearLayout.setOnClickListener {
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
                    Toast.makeText(this, "The permission has been obtained, you can download the picture", Toast.LENGTH_SHORT).show()
                } else {
                    if (PermissionManager.shouldShowRequestPermissionRationale(this)) {
                        PermissionManager.showPermissionDeniedDialog(this)
                    } else {
                        Toast.makeText(this, "Need to save pictures", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}