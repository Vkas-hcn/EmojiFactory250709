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
            performDownload()
        } else {
            Toast.makeText(this, "Storage permissions are required to save pictures", Toast.LENGTH_LONG).show()
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
            finish()
        }

        setupUI()
        loadMergedImage()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.imgBack.setOnClickListener {
            finish()
        }
    }


    private fun setupClickListeners() {
        binding.iconShare.setOnClickListener {
            shareImage(this)
        }

        binding.iconDownload.setOnClickListener {
            downloadImage()
        }
    }

    private fun shareImage(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            currentMergedBitmap?.let { bitmap ->
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

    private fun downloadImage() {
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownloadWithMediaStore()
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                performDownload()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun performDownloadWithMediaStore() {
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
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val fileName = "merged_emoji_${System.currentTimeMillis()}.png"
                val file = File(downloadsDir, fileName)

                val fileOutputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                fileOutputStream.close()

                Toast.makeText(this, "The image has been saved to the download folder: $fileName", Toast.LENGTH_LONG).show()

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

        currentMergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        currentMergedBitmap = null

        MergedImageHolder.mergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        MergedImageHolder.mergedBitmap = null
    }


    private fun loadMergedImage() {
        MergedImageHolder.mergedBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                currentMergedBitmap = bitmap
                binding.imgResult.setImageBitmap(bitmap)
            } else {
                binding.imgResult.setImageResource(R.drawable.face1)
                Toast.makeText(this, "Synthetic image has expired", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            binding.imgResult.setImageResource(R.drawable.face1)
            Toast.makeText(this, "No synthetic image found", Toast.LENGTH_SHORT).show()
        }
    }
}