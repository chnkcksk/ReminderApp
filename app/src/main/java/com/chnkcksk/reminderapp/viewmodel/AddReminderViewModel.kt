package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome : LiveData<Boolean> get() = _navigateHome


    fun addReminder(title:String, description:String){

        _isLoading.value = true

        val currentUser = auth.currentUser

        if (currentUser==null){
            _toastMessage.value = "User login required"
            return
        }
        val reminder = hashMapOf(
            "title" to title,
            "description" to  description,
            "timestamp" to System.currentTimeMillis(),
            "isCompleted" to false
        )

        firestore.collection("Users")
            .document(currentUser.uid)
            .collection("workspaces")
            .document("personalWorkspace")
            .collection("reminders")
            .add(reminder)
            .addOnSuccessListener {
                _isLoading.value = false
                _navigateHome.value = true
                _toastMessage.value = "Reminder saved"
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.value = "Reminder could not be saved!"
            }



    }

}