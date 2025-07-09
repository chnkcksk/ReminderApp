package com.chnkcksk.reminderapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.model.DrawerMenuItem

class DrawerMenuAdapter(
    private var items: ArrayList<DrawerMenuItem>,
    private val onItemClick: (DrawerMenuItem) -> Unit
) : RecyclerView.Adapter<DrawerMenuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.itemIcon)
        val text: TextView = view.findViewById(R.id.itemText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawer_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text.text = item.title

        // Workspace tipine göre icon ayarla
        when (item.workspaceType) {
            "Group" -> {
                // Personal workspace icon
                holder.icon.setImageResource(R.drawable.baseline_group_24)
            }
            "Personal" -> {
                // Shared workspace icon
                holder.icon.setImageResource(R.drawable.baseline_person_24)
            }
            else -> {
                // Default icon
                holder.icon.setImageResource(R.drawable.baseline_person_24)
            }
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    // Yeni workspace listesi geldiğinde adapter'ı güncelleme fonksiyonu
    fun updateList(newItems: ArrayList<DrawerMenuItem>) {
        android.util.Log.d("DrawerMenuAdapter", "updateList called with ${newItems.size} items")
        newItems.forEach { item ->
            android.util.Log.d("DrawerMenuAdapter", "Item: ${item.title} - ${item.workspaceType}")
        }

        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()

        android.util.Log.d("DrawerMenuAdapter", "Adapter updated, current item count: ${items.size}")
    }
}