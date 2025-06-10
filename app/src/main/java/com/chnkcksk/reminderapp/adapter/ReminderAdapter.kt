package com.chnkcksk.reminderapp.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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

class ReminderAdapter(
    private val context: Context,
    private val workspaceId: String,
    private val isReadOnly: String,
    private val owner:Boolean,
    private val homeReminderList: ArrayList<Reminder>,
    private val onItemClick: (Reminder) -> Unit


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

                loadingManager.showLoading(context)

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
                            loadingManager.dismissLoading()
                            Toast.makeText(
                                holder.itemView.context,
                                "Status updated: $isCompleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener {
                            loadingManager.dismissLoading()
                            Toast.makeText(holder.itemView.context, "Hata oluştu", Toast.LENGTH_SHORT)
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
                            Toast.makeText(holder.itemView.context, "Hata oluştu", Toast.LENGTH_SHORT)
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

        // Listener yeniden tanımla

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
}