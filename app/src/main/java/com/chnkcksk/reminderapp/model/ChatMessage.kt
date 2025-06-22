package com.chnkcksk.reminderapp.model

data class ChatMessage(
    val id: String = "",
    val senderId:String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
