package com.live.life.intoxication.dreams

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.live.life.intoxication.dreams.databinding.ActivityNetBinding
import com.live.life.intoxication.dreams.databinding.ActivityStartBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setting)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        binding.apply {
            imgBack.setOnClickListener {
                finish()
            }
            atvShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${this@SettingsActivity.packageName}")
                try {
                    startActivity(Intent.createChooser(intent, "Share via"))
                } catch (ex: Exception) {
                    // Handle error
                }
            }
            atvPlo.setOnClickListener {
                val intent = Intent(Intent .ACTION_VIEW)
                intent.data = Uri.parse("https://sites.google.com/view/drawmuse-launcher/home")
                startActivity(intent)
            }
        }
    }
}