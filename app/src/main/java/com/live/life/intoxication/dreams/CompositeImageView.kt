package com.live.life.intoxication.dreams

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView

class CompositeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var imgFace: ImageView
    private var imgEyes: ImageView
    private var imgMouth: ImageView

    init {
        inflate(context, R.layout.item_composite_emoji, this)
        imgFace = findViewById(R.id.img_face)
        imgEyes = findViewById(R.id.img_eyes)
        imgMouth = findViewById(R.id.img_mouth)
    }
}