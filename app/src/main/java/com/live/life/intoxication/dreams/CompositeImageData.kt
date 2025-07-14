package com.live.life.intoxication.dreams

data class CompositeImageData(
    val faceResId: Int,
    val eyeResId: Int,
    val mouthResId: Int
)

object CompositeImageDataGenerator {

    fun generateCompositeImageList(): List<CompositeImageData> {
        val faceList = ImageViewData.imageFace
        val eyeList = ImageViewData.imageEye
        val mouthList = ImageViewData.imageMouth

        val compositeList = mutableListOf<CompositeImageData>()

        for (i in faceList.indices) {
            val faceResId = faceList[i]
            val eyeResId = eyeList[i % eyeList.size]
            val mouthResId = mouthList[i % mouthList.size]

            compositeList.add(CompositeImageData(faceResId, eyeResId, mouthResId))
        }

        return compositeList
    }
}