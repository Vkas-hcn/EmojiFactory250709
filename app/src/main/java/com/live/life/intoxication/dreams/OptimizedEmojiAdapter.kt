package com.live.life.intoxication.dreams

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.live.life.intoxication.dreams.databinding.ItemCompositeEmojiBinding
import kotlinx.coroutines.*

sealed class EmojiItemData {
    data class SingleImage(val imageResId: Int) : EmojiItemData()
    data class CompositeImage(val data: CompositeImageData) : EmojiItemData()
}

class OptimizedEmojiAdapter(
    private val onItemClick: (EmojiItemData, Int) -> Unit
) : ListAdapter<EmojiItemData, RecyclerView.ViewHolder>(EmojiDiffCallback()) {

    private var selectedPosition = -1
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
                    val size = (48 * parent.context.resources.displayMetrics.density).toInt()
                    val params = ViewGroup.MarginLayoutParams(size, size)
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

                val size = (48 * parent.context.resources.displayMetrics.density).toInt()
                val params = ViewGroup.MarginLayoutParams(size, size)
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is CompositeImageViewHolder) {
            holder.cancelLoading()
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
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
        private var loadingJob: Job? = null

        fun bind(item: EmojiItemData.CompositeImage, position: Int, isSelected: Boolean) {
            cancelLoading()

            val cachedBitmap = OptimizedBitmapCache.getBitmap(
                OptimizedBitmapCache.generateKey(item.data)
            )

            if (cachedBitmap != null) {
                displayCompositeBitmap(cachedBitmap)
            } else {
                binding.imgFace.setImageResource(item.data.faceResId)
                binding.imgEyes.setImageResource(android.R.color.transparent)
                binding.imgMouth.setImageResource(android.R.color.transparent)

                loadingJob = coroutineScope.launch {
                    try {
                        val bitmap = BitmapComposer.createCompositeBitmapAsync(
                            itemView.context,
                            item.data,
                            48
                        )

                        bitmap?.let { displayCompositeBitmap(it) }
                    } catch (e: Exception) {
                        binding.imgFace.setImageResource(item.data.faceResId)
                        binding.imgEyes.setImageResource(item.data.eyeResId)
                        binding.imgMouth.setImageResource(item.data.mouthResId)
                    }
                }
            }

            setSelectionState(itemView, isSelected)
            itemView.setOnClickListener {
                onItemClick(item, position)
                updateSelection(position)
            }
        }

        private fun displayCompositeBitmap(bitmap: Bitmap) {
            binding.imgFace.setImageBitmap(bitmap)
            binding.imgEyes.setImageResource(android.R.color.transparent)
            binding.imgMouth.setImageResource(android.R.color.transparent)
        }

        fun cancelLoading() {
            loadingJob?.cancel()
            loadingJob = null
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

class EmojiDiffCallback : DiffUtil.ItemCallback<EmojiItemData>() {

    override fun areItemsTheSame(oldItem: EmojiItemData, newItem: EmojiItemData): Boolean {
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