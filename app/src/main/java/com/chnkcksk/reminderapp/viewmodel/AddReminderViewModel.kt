package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome: LiveData<Boolean> get() = _navigateHome

    private val _workspaceName = MutableLiveData<String>()
    val workspaceName: LiveData<String> get() = _workspaceName

    private val _setNotification = MutableLiveData<Boolean>()
    val setNotification: LiveData<Boolean> get() = _setNotification


    suspend fun addReminder(
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String,
        isNotificationChecked: Boolean

    ) {

        _isLoading.value = true

        val currentUser = auth.currentUser

        if (currentUser == null) {
            _toastMessage.value = "User login required"
            return
        }

        val reminder = hashMapOf(
            "title" to title,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "isCompleted" to false,
            "priority" to priority,
            "date" to date,
            "time" to time,
            "reminder" to isNotificationChecked
        )

        try {

            firestore.collection("Users")
                .document(currentUser.uid)
                .collection("workspaces")
                .document("personalWorkspace")
                .collection("reminders")
                .add(reminder)
                .await()


            if (isNotificationChecked) {
                _setNotification.value = true
            }


            _isLoading.value = false
            delay(1200)
            _toastMessage.value = "Reminder saved"
            delay(500)
            _navigateHome.value = true


        } catch (e: Exception) {
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = "Reminder could not be saved!"
        }


    }

}