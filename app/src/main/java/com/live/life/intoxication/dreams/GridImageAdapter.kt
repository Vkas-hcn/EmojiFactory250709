package com.live.life.intoxication.dreams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class GridImageAdapter(
    private val imageList: List<Int>,
    private val onItemClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<GridImageAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.img_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grid_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.setImageResource(imageList[position])
        holder.itemView.setOnClickListener {
            onItemClick(position, imageList[position])
        }
    }

    override fun getItemCount(): Int = imageList.size
}