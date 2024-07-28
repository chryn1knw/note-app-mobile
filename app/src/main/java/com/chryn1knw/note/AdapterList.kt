package com.chryn1knw.note

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdapterList(private val itemLists: List<ItemList>) : RecyclerView.Adapter<AdapterList.ViewHolder>() {
    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(item: ItemList)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val imageView: ImageView = itemView.findViewById(R.id.item_image)
        val judul: TextView = itemView.findViewById(R.id.title)
        val subJudul: TextView = itemView.findViewById(R.id.sub_title)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemLists[position]

        holder.judul.text = if (item.judul.length > 15) item.judul.substring(0, 15) + "..." else item.judul
        holder.subJudul.text = if (item.subJudul.length > 20) item.subJudul.substring(0, 20) + "..." else item.subJudul
        holder.tvTime.text = item.timestamp

        if (item.imageUrl.isNullOrEmpty()) {
            holder.imageView.visibility = View.GONE
        } else {
            holder.imageView.visibility = View.VISIBLE
            Glide.with(holder.imageView.context).load(item.imageUrl).into(holder.imageView)
        }

        holder.cardView.setOnClickListener {
            listener?.onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return itemLists.size
    }
}
