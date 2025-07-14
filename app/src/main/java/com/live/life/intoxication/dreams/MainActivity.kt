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
        private const val MAIN_PAGE_DISPLAY_COUNT = 5

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
            finish()
        }

        setupRecyclerViews()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        binding.rvFruit.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = GridImageAdapter(ImageViewData.downloadImageFruits.take(MAIN_PAGE_DISPLAY_COUNT)) { position, imageRes ->
                navigateToDetail(TYPE_FRUITS)
            }
        }

        binding.rvSmile.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = GridImageAdapter(ImageViewData.downloadImageSmile.take(MAIN_PAGE_DISPLAY_COUNT)) { position, imageRes ->
                navigateToDetail(TYPE_SMILE)
            }
        }

        binding.rvAlien.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            adapter = GridImageAdapter(ImageViewData.downloadImageAlien.take(MAIN_PAGE_DISPLAY_COUNT)) { position, imageRes ->
                navigateToDetail(TYPE_ALIEN)
            }
        }
    }

    private fun setupClickListeners() {
        binding.imgMix.setOnClickListener {
            startActivity(Intent(this@MainActivity, MixActivity::class.java))
        }

        binding.imgSetting.setOnClickListener {
            lifecycleScope.launch {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
        binding.mcvFruit.setOnClickListener {
            navigateToDetail(TYPE_FRUITS)
        }
        binding.mcvSmile.setOnClickListener {
            navigateToDetail(TYPE_SMILE)
        }
        binding.mcvAlien.setOnClickListener {
            navigateToDetail(TYPE_ALIEN)
        }
    }

    private fun navigateToDetail(type: String) {
        val intent = Intent(this, DownDetailActivity::class.java).apply {
            putExtra(EXTRA_IMAGE_TYPE, type)
        }
        startActivity(intent)
    }
}