package com.live.life.intoxication.dreams

data class CompositeImageData(
    val faceResId: Int,
    val eyeResId: Int,
    val mouthResId: Int
)

object CompositeImageDataGenerator {

    /**
     * 生成复合图像数据，确保每次生成的结果都一样
     * 数量与imageFace保持一致
     */
    fun generateCompositeImageList(): List<CompositeImageData> {
        val faceList = ImageViewData.imageFace
        val eyeList = ImageViewData.imageEye
        val mouthList = ImageViewData.imageMouth

        val compositeList = mutableListOf<CompositeImageData>()

        for (i in faceList.indices) {
            val faceResId = faceList[i]
            // 使用固定的算法确保每次结果一致
            val eyeResId = eyeList[i % eyeList.size]
            val mouthResId = mouthList[i % mouthList.size]

            compositeList.add(CompositeImageData(faceResId, eyeResId, mouthResId))
        }

        return compositeList
    }
}