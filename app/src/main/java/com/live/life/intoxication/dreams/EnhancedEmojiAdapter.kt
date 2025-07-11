package com.live.life.intoxication.dreams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.live.life.intoxication.dreams.databinding.ItemCompositeEmojiBinding

sealed class EmojiItemData {
    data class SingleImage(val imageResId: Int) : EmojiItemData()
    data class CompositeImage(val data: CompositeImageData) : EmojiItemData()
}

class EnhancedEmojiAdapter(
    private val onItemClick: (EmojiItemData, Int) -> Unit
) : ListAdapter<EmojiItemData, RecyclerView.ViewHolder>(EmojiDiffCallback()) {

    private var selectedPosition = -1

    companion object {
        const val TYPE_SINGLE = 0
        const val TYPE_COMPOSITE = 1
    }

    fun updateData(newItemList: List<EmojiItemData>) {
        selectedPosition = -1
        submitList(newItemList)
    }

    fun updateSelection(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position

        if (previousPosition != -1 && previousPosition < itemCount) {
            notifyItemChanged(previousPosition)
        }
        if (position != -1 && position < itemCount) {
            notifyItemChanged(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EmojiItemData.SingleImage -> TYPE_SINGLE
            is EmojiItemData.CompositeImage -> TYPE_COMPOSITE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SINGLE -> {
                val imageView = ImageView(parent.context).apply {
                    // 设置固定尺寸48dp
                    val size = (48 * parent.context.resources.displayMetrics.density).toInt()
                    val params = ViewGroup.MarginLayoutParams(size, size)
                    // 设置左右间距21dp，上下间距8dp
                    val marginHorizontal = (21 * parent.context.resources.displayMetrics.density).toInt()
                    val marginVertical = (8 * parent.context.resources.displayMetrics.density).toInt()
                    params.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
                    layoutParams = params

                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
                SingleImageViewHolder(imageView)
            }
            TYPE_COMPOSITE -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_composite_emoji, parent, false)

                // 设置固定尺寸48dp
                val size = (48 * parent.context.resources.displayMetrics.density).toInt()
                val params = ViewGroup.MarginLayoutParams(size, size)
                // 设置左右间距21dp，上下间距8dp
                val marginHorizontal = (21 * parent.context.resources.displayMetrics.density).toInt()
                val marginVertical = (8 * parent.context.resources.displayMetrics.density).toInt()
                params.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
                itemView.layoutParams = params

                CompositeImageViewHolder(itemView)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SingleImageViewHolder -> holder.bind(
                item as EmojiItemData.SingleImage,
                position,
                selectedPosition == position
            )
            is CompositeImageViewHolder -> holder.bind(
                item as EmojiItemData.CompositeImage,
                position,
                selectedPosition == position
            )
        }
    }

    inner class SingleImageViewHolder(private val imageView: ImageView) :
        RecyclerView.ViewHolder(imageView) {

        fun bind(item: EmojiItemData.SingleImage, position: Int, isSelected: Boolean) {
            imageView.setImageResource(item.imageResId)
            setSelectionState(imageView, isSelected)
            imageView.setOnClickListener {
                onItemClick(item, position)
                updateSelection(position)
            }
        }
    }

    inner class CompositeImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemCompositeEmojiBinding.bind(itemView)

        fun bind(item: EmojiItemData.CompositeImage, position: Int, isSelected: Boolean) {
            binding.imgFace.setImageResource(item.data.faceResId)
            binding.imgEyes.setImageResource(item.data.eyeResId)
            binding.imgMouth.setImageResource(item.data.mouthResId)

            setSelectionState(itemView, isSelected)
            itemView.setOnClickListener {
                onItemClick(item, position)
                updateSelection(position)
            }
        }
    }

    private fun setSelectionState(view: View, isSelected: Boolean) {
        if (isSelected) {
            view.alpha = 1.0f
            view.scaleX = 1.05f
            view.scaleY = 1.05f
            view.setBackgroundResource(R.drawable.selected_border)
        } else {
            view.alpha = 0.8f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.setBackgroundResource(android.R.color.transparent)
        }
    }
}

// DiffUtil.ItemCallback用于比较列表项的差异
class EmojiDiffCallback : DiffUtil.ItemCallback<EmojiItemData>() {

    override fun areItemsTheSame(oldItem: EmojiItemData, newItem: EmojiItemData): Boolean {
        // 检查是否是同一个项目（通过类型和内容判断）
        return when {
            oldItem is EmojiItemData.SingleImage && newItem is EmojiItemData.SingleImage -> {
                oldItem.imageResId == newItem.imageResId
            }
            oldItem is EmojiItemData.CompositeImage && newItem is EmojiItemData.CompositeImage -> {
                oldItem.data.faceResId == newItem.data.faceResId &&
                        oldItem.data.eyeResId == newItem.data.eyeResId &&
                        oldItem.data.mouthResId == newItem.data.mouthResId
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: EmojiItemData, newItem: EmojiItemData): Boolean {
        // 检查内容是否相同
        return when {
            oldItem is EmojiItemData.SingleImage && newItem is EmojiItemData.SingleImage -> {
                oldItem.imageResId == newItem.imageResId
            }
            oldItem is EmojiItemData.CompositeImage && newItem is EmojiItemData.CompositeImage -> {
                oldItem.data == newItem.data
            }
            else -> false
        }
    }
}