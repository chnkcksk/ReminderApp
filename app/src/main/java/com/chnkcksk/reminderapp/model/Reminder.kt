package com.chnkcksk.reminderapp.model

data class Reminder(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val timestamp: String = "",
    val priority: String = "",
    val date: String = "",
    val time: String = ""
)