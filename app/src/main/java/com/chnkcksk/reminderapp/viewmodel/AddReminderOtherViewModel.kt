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

class AddReminderOtherViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateWorkspace = MutableLiveData<Boolean>()
    val navigateWorkspace: LiveData<Boolean> get() = _navigateWorkspace

    private val _workspaceName = MutableLiveData<String>()
    val workspaceName: LiveData<String> get() = _workspaceName

    private val _workspaceType = MutableLiveData<String>()
    val workspaceType: LiveData<String> get() = _workspaceType

    suspend fun addOtherReminder(
        workspaceId: String,
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            _toastMessage.value = "Error"
            return
        }

        _isLoading.value = true

        val reminder = hashMapOf(
            "title" to title,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "isCompleted" to false,
            "priority" to priority,
            "date" to date,
            "time" to time
        )

        try {
            // Firestore işlemini coroutine ile yapıyoruz
            firestore.collection("workspaces")
                .document(workspaceId)
                .collection("reminders")
                .add(reminder)
                .await() // Coroutine ile bekleme

            // Başarılı durumda
            _isLoading.value = false
            delay(1200) // 0.5 saniye bekle
            _toastMessage.value = "Reminder added successfully"
            delay(500) // 0.5 saniye bekle
            _navigateWorkspace.value = true

        } catch (e: Exception) {
            // Hata durumunda
            _isLoading.value = false
            _toastMessage.value = "Reminder could not be added"
        }
    }

    fun getDatas(workspaceId: String){

        _isLoading.value = true

        firestore.collection("workspaces")
            .document(workspaceId)
            .get()
            .addOnSuccessListener { doc ->
                _workspaceName.value = doc.getString("workspaceName") ?: ""
                _workspaceType.value = doc.getString("workspaceType") ?: ""
                _isLoading.value = false

            }.addOnFailureListener {
                _isLoading.value = false

            }

    }



















}