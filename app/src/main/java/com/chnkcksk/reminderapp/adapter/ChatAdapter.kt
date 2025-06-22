package com.chnkcksk.reminderapp.adapter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chnkcksk.reminderapp.databinding.ItemChatReceivedBinding
import com.chnkcksk.reminderapp.databinding.ItemChatSentBinding
import com.chnkcksk.reminderapp.model.ChatMessage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChatAdapter(
    private val onMessageLongClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = ArrayList<ChatMessage>()

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    fun submitList(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val currentUserId = Firebase.auth.currentUser?.uid
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding =
                ItemChatSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageViewHolder(binding)
        } else {
            val binding =
                ItemChatReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(private val binding: ItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.messageTextView.text = message.message

            val initials = message.senderName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }.joinToString("").take(2)

            val avatarBitmap = createInitialsAvatar(
                initials = initials,
                size = 200,
                backgroundColor = Color.parseColor("#DFCEA0"),
                textColor = Color.WHITE
            )

            binding.profileImage.setImageBitmap(avatarBitmap)

            binding.messageTextView.setOnLongClickListener {
                onMessageLongClick(message) // dışarı ile iletişim kur
                true
            }


            // binding.timeTextView.text = mesaj zamanı göstermek istersen buraya ekle
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.senderNameTextView.text = message.senderName
            binding.messageTextView.text = message.message

            val initials = message.senderName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }.joinToString("").take(2)

            val avatarBitmap = createInitialsAvatar(
                initials = initials,
                size = 200,
                backgroundColor = Color.parseColor("#DFCEA0"),
                textColor = Color.WHITE
            )

            binding.profileImage.setImageBitmap(avatarBitmap)

            // binding.timeTextView.text = mesaj zamanı göstermek istersen buraya ekle
        }
    }

    fun createInitialsAvatar(
        initials: String, size: Int, backgroundColor: Int, textColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Arka plan
        val paint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Yazı (baş harfler)
        paint.apply {
            color = textColor
            textSize = size / 2f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val xPos = size / 2f
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(initials, xPos, yPos, paint)

        return bitmap
    }
}
