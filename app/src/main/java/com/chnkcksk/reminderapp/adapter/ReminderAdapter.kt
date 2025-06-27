package com.chnkcksk.reminderapp.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.ReminderRecyclerRowBinding
import com.chnkcksk.reminderapp.model.Reminder
import com.chnkcksk.reminderapp.util.LoadingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.appcompat.app.AlertDialog

class ReminderAdapter(
    private val context: Context,
    private val workspaceId: String,
    private val isReadOnly: String,
    private val owner:Boolean,
    private val homeReminderList: ArrayList<Reminder>,
    private val onItemClick: (Reminder) -> Unit,
    private val onItemDelete: ((Reminder, Int) -> Unit)? = null // Silme callback'i eklendi

) :
    RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val loadingManager = LoadingManager.getInstance()

    class ReminderViewHolder(val binding: ReminderRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding =
            ReminderRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return homeReminderList.size
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {

        holder.binding.reminderTitleTV.text = homeReminderList[position].title
        holder.binding.reminderDateTV.text = "${homeReminderList[position].date}, "
        holder.binding.reminderTimeTV.text = homeReminderList[position].time

        val priority = homeReminderList[position].priority

        if (priority == "High") {
            holder.binding.priorityTV.text = "H"
            holder.binding.priorityTV.setTextColor(ContextCompat.getColor(context, R.color.red))
        } else if (priority == "Medium") {
            holder.binding.priorityTV.text = "M"
            holder.binding.priorityTV.setTextColor(ContextCompat.getColor(context, R.color.orange))
        } else if (priority == "Low") {
            holder.binding.priorityTV.text = "L"
            holder.binding.priorityTV.setTextColor(ContextCompat.getColor(context, R.color.green))
        }else{
            holder.binding.priorityTV.isVisible = false
        }

        val reminder = homeReminderList[position].reminder

        if (reminder == true){
            holder.binding.reminderIV.setImageResource(R.drawable.baseline_notifications_none_24)
        }

        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClick(homeReminderList[position]) // Reminder nesnesini gönder
            }
        }

        val isCompleted = homeReminderList[position].isCompleted == true

        if (isCompleted) {
            holder.binding.reminderTitleTV.paintFlags =
                holder.binding.reminderTitleTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.reminderDateTV.paintFlags =
                holder.binding.reminderDateTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.reminderTimeTV.paintFlags =
                holder.binding.reminderTimeTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.priorityTV.paintFlags =
                holder.binding.priorityTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.binding.reminderTitleTV.paintFlags =
                holder.binding.reminderTitleTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.reminderDateTV.paintFlags =
                holder.binding.reminderDateTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.reminderTimeTV.paintFlags =
                holder.binding.reminderTimeTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.binding.priorityTV.paintFlags =
                holder.binding.priorityTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Listener eklemeden önce temizle
        holder.binding.switchButton.setOnCheckedChangeListener(null)

        // Mevcut durumu göster
        holder.binding.switchButton.isChecked = homeReminderList[position].isCompleted == true
        updateSwitchColor(holder, homeReminderList[position].isCompleted == true)

        if (isReadOnly == "Read only" && owner==false) {
            holder.binding.switchButton.isEnabled = false
            updateSwitchColor(holder, homeReminderList[position].isCompleted == true)
        }else{
            holder.binding.switchButton.setOnCheckedChangeListener { _, isChecked ->
                updateSwitchColor(holder, isChecked)

                // Firebase'e kaydet
                val reminderId = homeReminderList[position].id // her öğenin benzersiz bir ID'si olmalı

                val currentUser = auth.currentUser

                //loadingManager.showLoading(context)

                if (currentUser == null) {
                    return@setOnCheckedChangeListener
                }
                if (workspaceId == "personalWorkspace") {
                    val userId = currentUser.uid
                    firestore.collection("Users")
                        .document(userId)
                        .collection("workspaces")
                        .document(workspaceId)
                        .collection("reminders")
                        .document(reminderId)
                        .update("isCompleted", isChecked)
                        .addOnSuccessListener {
                            //loadingManager.dismissLoading()
                            Toast.makeText(
                                holder.itemView.context,
                                "Status updated: $isCompleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener {
                            //loadingManager.dismissLoading()
                            Toast.makeText(holder.itemView.context, "Error occurred", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    firestore.collection("workspaces")
                        .document(workspaceId)
                        .collection("reminders")
                        .document(reminderId)
                        .update("isCompleted", isChecked)
                        .addOnSuccessListener {
                            loadingManager.dismissLoading()
                            Toast.makeText(
                                holder.itemView.context,
                                "Status updated: $isCompleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener {
                            loadingManager.dismissLoading()
                            Toast.makeText(holder.itemView.context, "Error occurred", Toast.LENGTH_SHORT)
                                .show()
                        }
                }

                if (isChecked) {
                    holder.binding.reminderTitleTV.paintFlags =
                        holder.binding.reminderTitleTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holder.binding.reminderDateTV.paintFlags =
                        holder.binding.reminderDateTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holder.binding.reminderTimeTV.paintFlags =
                        holder.binding.reminderTimeTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holder.binding.priorityTV.paintFlags =
                        holder.binding.priorityTV.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    holder.binding.reminderTitleTV.paintFlags =
                        holder.binding.reminderTitleTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    holder.binding.reminderDateTV.paintFlags =
                        holder.binding.reminderDateTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    holder.binding.reminderTimeTV.paintFlags =
                        holder.binding.reminderTimeTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    holder.binding.priorityTV.paintFlags =
                        holder.binding.priorityTV.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

            }
        }
    }

    private fun updateSwitchColor(holder: ReminderAdapter.ReminderViewHolder, isChecked: Boolean) {
        val context = holder.itemView.context
        if (isChecked) {
            val greenColor = ContextCompat.getColor(context, R.color.slider_color)
            holder.binding.switchButton.thumbTintList = ColorStateList.valueOf(greenColor)
            holder.binding.switchButton.trackTintList = ColorStateList.valueOf(greenColor)
        } else {
            val grayColor = ContextCompat.getColor(context, R.color.secondary_color)
            holder.binding.switchButton.thumbTintList = ColorStateList.valueOf(grayColor)
            holder.binding.switchButton.trackTintList = ColorStateList.valueOf(grayColor)
        }
    }

    fun updateList(newList: ArrayList<Reminder>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = homeReminderList.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return homeReminderList[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return homeReminderList[oldItemPosition] == newList[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        homeReminderList.clear()
        homeReminderList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    // Silme işlemi için yardımcı fonksiyon
    fun deleteItem(position: Int) {
        if (position >= 0 && position < homeReminderList.size) {
            val reminder = homeReminderList[position]
            onItemDelete?.invoke(reminder, position)
        }
    }

    // Firebase'den silme işlemi
    fun deleteReminderFromFirebase(reminder: Reminder, position: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User login required", Toast.LENGTH_SHORT).show()
            return
        }

        loadingManager.showLoading(context)

        if (workspaceId == "personalWorkspace") {
            val userId = currentUser.uid
            firestore.collection("Users")
                .document(userId)
                .collection("workspaces")
                .document(workspaceId)
                .collection("reminders")
                .document(reminder.id)
                .delete()
                .addOnSuccessListener {
                    loadingManager.dismissLoading()
                    homeReminderList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, homeReminderList.size)
                    Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    loadingManager.dismissLoading()
                    Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            firestore.collection("workspaces")
                .document(workspaceId)
                .collection("reminders")
                .document(reminder.id)
                .delete()
                .addOnSuccessListener {
                    loadingManager.dismissLoading()
                    homeReminderList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, homeReminderList.size)
                    Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    loadingManager.dismissLoading()
                    Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ItemTouchHelper için swipe callback
    fun getSwipeCallback(): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            private val deleteIcon: Drawable = ContextCompat.getDrawable(context, R.drawable.baseline_delete_outline_24_w)!!
            private val background = ColorDrawable(ContextCompat.getColor(context, R.color.red))
            private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                // Read-only durumunda silme işlemine izin verme
                if (isReadOnly == "Read only" && !owner) {
                    notifyItemChanged(position) // Item'ı eski haline döndür
                    Toast.makeText(context, "Bu çalışma alanında silme yetkiniz yok", Toast.LENGTH_SHORT).show()
                    return
                }

                // Silme onayı için dialog göster
                showDeleteConfirmationDialog(position)
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.3f // Swipe için gereken minimum mesafe
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                val itemView = viewHolder.itemView
                val itemHeight = itemView.height
                val isCancelled = dX == 0f && !isCurrentlyActive

                if (isCancelled) {
                    clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                // Sadece sola kaydırma (dX < 0) durumunda kırmızı arka plan çiz
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    // Silme ikonunu sağda göster
                    val iconMargin = (itemHeight - deleteIcon.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - deleteIcon.intrinsicWidth

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
                c.drawRect(left, top, right, bottom, clearPaint)
            }
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        if (position >= homeReminderList.size) {
            notifyItemChanged(position)
            return
        }

        val reminder = homeReminderList[position]

        AlertDialog.Builder(context)
            .setTitle("Deletion Confirmation")
            .setMessage("Are you sure you want to delete the reminder '${reminder.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReminderFromFirebase(reminder, position)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Item'ı eski haline döndür
                notifyItemChanged(position)
            }
            .setOnCancelListener {
                // Dialog iptal edilirse item'ı eski haline döndür
                notifyItemChanged(position)
            }
            .show()
    }
}