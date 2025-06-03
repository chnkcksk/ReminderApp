package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chnkcksk.reminderapp.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _reminderList = MutableLiveData<ArrayList<Reminder>>()
    val reminderList: LiveData<ArrayList<Reminder>> get() = _reminderList


    fun loadRemindersList() {
        val currentUser = auth.currentUser

        _isLoading.value = true

        if (currentUser != null) {

            firestore.collection("Users")
                .document(currentUser.uid)
                .collection("workspaces")
                .document("personalWorkspace")
                .collection("reminders")
                .get()
                .addOnSuccessListener { documents ->
                    val reminderList = ArrayList<Reminder>()

                    documents.forEach { document ->
                        val reminder = Reminder(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            description = document.getString("description") ?: "",
                            isCompleted = document.getBoolean("isCompleted") ?: false,
                            timestamp = document.get("timestamp").toString() ?: ""
                        )
                        reminderList.add(reminder)
                    }
                    reminderList.sortByDescending { reminder ->
                        try {
                            reminder.timestamp.toLong()
                        } catch (e: Exception) {
                            0L // hata durumunda varsayılan değer
                        }
                    }

                    _reminderList.value = reminderList
                    _isLoading.value = false

                }.addOnFailureListener { e ->
                    _isLoading.value = false
                    _toastMessage.value = e.toString()
                }

        }

    }


}