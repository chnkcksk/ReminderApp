package com.chnkcksk.reminderapp.model

data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val timestamp: String
)