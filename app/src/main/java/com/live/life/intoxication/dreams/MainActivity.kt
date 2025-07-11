package com.live.life.intoxication.dreams

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.live.life.intoxication.dreams.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val EXTRA_IMAGE_TYPE = "image_type"
        const val TYPE_FRUITS = "fruits"
        const val TYPE_SMILE = "smile"
        const val TYPE_ALIEN = "alien"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()

        onBackPressedDispatcher.addCallback {
            // 可以在这里添加退出确认逻辑
        }

        setupRecyclerViews()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        // 设置水果图片列表
        binding.rvFruit.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 6)
            adapter = GridImageAdapter(ImageViewData.downloadImageFruits) { position, imageRes ->
                navigateToDetail(TYPE_FRUITS)
            }
        }

        // 设置笑脸图片列表
        binding.rvSmile.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 6)
            adapter = GridImageAdapter(ImageViewData.downloadImageSmile) { position, imageRes ->
                navigateToDetail(TYPE_SMILE)
            }
        }

        // 设置外星人图片列表
        binding.rvAlien.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 6)
            adapter = GridImageAdapter(ImageViewData.downloadImageAlien) { position, imageRes ->
                navigateToDetail(TYPE_ALIEN)
            }
        }
    }

    private fun setupClickListeners() {
        binding.imgMix.setOnClickListener {
            lifecycleScope.launch {
                startActivity(Intent(this@MainActivity, MixActivity::class.java))
            }
        }

        binding.imgSetting.setOnClickListener {
            lifecycleScope.launch {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
    }

    private fun navigateToDetail(type: String) {
        val intent = Intent(this, DownDetailActivity::class.java).apply {
            putExtra(EXTRA_IMAGE_TYPE, type)
        }
        startActivity(intent)
    }
}