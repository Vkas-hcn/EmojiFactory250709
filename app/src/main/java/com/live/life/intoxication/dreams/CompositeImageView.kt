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

    fun setCompositeData(data: CompositeImageData) {
        imgFace.setImageResource(data.faceResId)
        imgEyes.setImageResource(data.eyeResId)
        imgMouth.setImageResource(data.mouthResId)
    }

    fun setCompositeDataWithVisibility(
        data: CompositeImageData,
        showFace: Boolean = true,
        showEyes: Boolean = true,
        showMouth: Boolean = true
    ) {
        if (showFace) {
            imgFace.setImageResource(data.faceResId)
            imgFace.visibility = VISIBLE
        } else {
            imgFace.visibility = GONE
        }

        if (showEyes) {
            imgEyes.setImageResource(data.eyeResId)
            imgEyes.visibility = VISIBLE
        } else {
            imgEyes.visibility = GONE
        }

        if (showMouth) {
            imgMouth.setImageResource(data.mouthResId)
            imgMouth.visibility = VISIBLE
        } else {
            imgMouth.visibility = GONE
        }
    }

    fun setAlphaLevels(faceAlpha: Float = 1.0f, eyesAlpha: Float = 1.0f, mouthAlpha: Float = 1.0f) {
        imgFace.alpha = faceAlpha
        imgEyes.alpha = eyesAlpha
        imgMouth.alpha = mouthAlpha
    }

    // 获取复合图像的bitmap
    fun getCompositeBitmap(targetSize: Int = 96): android.graphics.Bitmap? {
        return try {
            val bitmap = android.graphics.Bitmap.createBitmap(targetSize, targetSize, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            // 临时设置视图尺寸
            val originalLayoutParams = layoutParams
            layoutParams = LayoutParams(targetSize, targetSize)
            measure(
                MeasureSpec.makeMeasureSpec(targetSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(targetSize, MeasureSpec.EXACTLY)
            )
            layout(0, 0, targetSize, targetSize)

            // 绘制到canvas
            draw(canvas)

            // 恢复原始布局参数
            layoutParams = originalLayoutParams

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}