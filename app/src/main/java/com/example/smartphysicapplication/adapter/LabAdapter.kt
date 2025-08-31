package com.example.smartphysicapplication.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartphysicapplication.R

// Model + Adapter
sealed class LabItem {
    data object New : LabItem()
    data object Custom : LabItem()
    data class Existing(val id: String, val name: String, val imageRes: Int?) : LabItem()
}

class LabAdapter(
    private val onClick: (LabItem) -> Unit
) : ListAdapter<LabItem, LabVH>(object : DiffUtil.ItemCallback<LabItem>() {
    override fun areItemsTheSame(o: LabItem, n: LabItem) =
        (o is LabItem.New && n is LabItem.New) ||
                (o is LabItem.Custom && n is LabItem.Custom) ||
                    (o is LabItem.Existing && n is LabItem.Existing && o.id == n.id)

    override fun areContentsTheSame(o: LabItem, n: LabItem) = o == n
}) {
    override fun onCreateViewHolder(p: ViewGroup, viewType: Int): LabVH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_lab_square, p, false)

        (v.layoutParams as RecyclerView.LayoutParams).apply {
            // chiều cao sẽ được set trong onBind bằng ratio trick
        }
        return LabVH(v, onClick)
    }
    override fun onBindViewHolder(h: LabVH, pos: Int) = h.bind(getItem(pos))
}

class LabVH(
    itemView: View,
    private val onClick: (LabItem) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val img = itemView.findViewById<ImageView>(R.id.imgCenter)
    private val title = itemView.findViewById<TextView>(R.id.tvTitle)

    fun bind(item: LabItem) {
        when (item) {
            LabItem.New -> {
                img.setImageResource(R.drawable.ic_add_large)
                title.text = "Thí nghiệm mới"
                val density = img.context.resources.displayMetrics.density
                img.layoutParams.width = (60 * density).toInt()
                img.layoutParams.height = (60 * density).toInt()
            }
            LabItem.Custom -> {
                img.setImageResource(R.drawable.ic_link)
                title.text = "Bản sao thí nghiệm"
                val density = img.context.resources.displayMetrics.density
                img.layoutParams.width = (60 * density).toInt()
                img.layoutParams.height = (60 * density).toInt()
            }
            is LabItem.Existing -> {
                if (item.imageRes != null) img.setImageResource(item.imageRes)
                else img.setImageResource(R.drawable.img_mindmap_c2)
                title.text = item.name

                img.post {
                    val parent = img.parent as View
                    val density = img.context.resources.displayMetrics.density
                    img.layoutParams.width = (parent.width * 0.9f).toInt()
                    img.layoutParams.height = (100 * density).toInt()
                    img.requestLayout()
                }
            }
        }
        itemView.setOnClickListener { onClick(item) }
    }
}
