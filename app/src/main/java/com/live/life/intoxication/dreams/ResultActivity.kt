package com.live.life.intoxication.dreams

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.live.life.intoxication.dreams.databinding.ActivityResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var currentMergedBitmap: Bitmap? = null

    // 权限请求launcher
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限被授予，执行下载
            performDownload()
        } else {
            // 权限被拒绝
            Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()

        onBackPressedDispatcher.addCallback {
            // 返回时清理资源
            finish()
        }

        setupUI()
        loadMergedImage()
        setupClickListeners()
    }

    private fun setupUI() {
        // 设置返回按钮点击事件
        binding.imgBack.setOnClickListener {
            finish()
        }
    }


    private fun setupClickListeners() {
        // 分享按钮点击事件
        binding.iconShare.setOnClickListener {
            shareImage(this)
        }

        // 下载按钮点击事件
        binding.iconDownload.setOnClickListener {
            downloadImage()
        }
    }

    private fun shareImage(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            currentMergedBitmap?.let { bitmap ->
                try {
                    // 创建临时文件
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

                    // 获取文件URI
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    // 创建分享Intent
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

    private fun downloadImage() {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 及以上版本不需要存储权限（使用MediaStore）
            performDownloadWithMediaStore()
        } else {
            // Android 9 及以下版本需要存储权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                performDownload()
            } else {
                // 请求权限
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun performDownloadWithMediaStore() {
        // Android 10+ 使用 MediaStore API
        currentMergedBitmap?.let { bitmap ->
            try {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                        "merged_emoji_${System.currentTimeMillis()}.png"
                    )
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES
                        )
                    }
                }

                val imageUri = resolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        Toast.makeText(this, "Images saved to album", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Saving failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No pictures to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDownload() {
        currentMergedBitmap?.let { bitmap ->
            try {
                // 创建下载目录
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                // 创建文件
                val fileName = "merged_emoji_${System.currentTimeMillis()}.png"
                val file = File(downloadsDir, fileName)

                // 保存bitmap到文件
                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                Toast.makeText(this, "The image has been saved to the download folder: $fileName", Toast.LENGTH_LONG).show()

                // 通知媒体扫描器更新
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Saving failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "No storage permissions", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No pictures to save", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 更安全的bitmap回收处理
        currentMergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        currentMergedBitmap = null

        // 清理MergedImageHolder中的bitmap
        MergedImageHolder.mergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        MergedImageHolder.mergedBitmap = null
    }

    /**
     * 优化的loadMergedImage方法
     */
    private fun loadMergedImage() {
        // 从MergedImageHolder获取合成的图片
        MergedImageHolder.mergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                currentMergedBitmap = bitmap
                binding.imgResult.setImageBitmap(bitmap)
            } else {
                // 如果bitmap已经被回收，显示默认图片
                binding.imgResult.setImageResource(R.drawable.face1)
                Toast.makeText(this, "合成图片已失效", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // 如果没有合成图片，显示默认图片
            binding.imgResult.setImageResource(R.drawable.face1)
            Toast.makeText(this, "未找到合成图片", Toast.LENGTH_SHORT).show()
        }
    }
}