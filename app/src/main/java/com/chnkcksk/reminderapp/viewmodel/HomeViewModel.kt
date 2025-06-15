package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.model.DrawerMenuItem
import com.chnkcksk.reminderapp.model.Reminder
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val notificationPermissionManager = NotificationPermissionManager.getInstance()

    private val _isLoading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _reminderList = MutableLiveData<ArrayList<Reminder>>()
    val reminderList: LiveData<ArrayList<Reminder>> get() = _reminderList

    private val _workspaceList = MutableLiveData<ArrayList<DrawerMenuItem>>()
    val workspaceList: LiveData<ArrayList<DrawerMenuItem>> get() = _workspaceList

    private val _isGoogleUser = MutableLiveData<Boolean>()
    val isGoogleUser: LiveData<Boolean> get() = _isGoogleUser


    fun getUserProviderData() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            _toastMessage.value = "Current user is null"
            return
        }

        currentUser.providerData.forEach { profile ->
            if (profile.providerId == "google.com") {
                _isGoogleUser.value = true
            }
        }

    }


    fun loadRemindersList() {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            _isLoading.value = true

            if (currentUser == null) {
                _toastMessage.value = "Error"
                return@launch
            }

            try {

                val documents = firestore.collection("Users")
                    .document(currentUser.uid)
                    .collection("workspaces")
                    .document("personalWorkspace")
                    .collection("reminders")
                    .get()
                    .await()

                val reminderList = ArrayList<Reminder>()

                documents.forEach { document ->
                    val reminder = Reminder(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        isCompleted = document.getBoolean("isCompleted") ?: false,
                        timestamp = document.get("timestamp").toString() ?: "",
                        priority = document.getString("priority") ?: "",
                        date = document.getString("date") ?: "",
                        time = document.getString("time") ?: "",
                        reminder = document.getBoolean("reminder") ?: false
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

            } catch (e: Exception) {

                _isLoading.value = false
                _toastMessage.value = "Failed to load reminders: $e"
                println("Failed to load reminders: $e")

            }

            /*
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
                            timestamp = document.get("timestamp").toString() ?: "",
                            priority = document.getString("priority") ?: "",
                            date = document.getString("date") ?: "",
                            time = document.getString("time") ?: "",
                            reminder = document.getBoolean("reminder") ?: false
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

             */
        }

    }

    // HomeViewModel.kt - loadWorkspaces() fonksiyonuna debug ekleyin


    fun loadWorkspaces() {

        viewModelScope.launch {


            val currentUser = auth.currentUser
            android.util.Log.d("HomeViewModel", "loadWorkspaces called")

            if (currentUser == null) {
                _toastMessage.value = "Error"
                return@launch
            }

            val userId = currentUser.uid

            _isLoading.value = true

            try {
                val documents = firestore.collection("workspaces")
                    .whereArrayContains("members", userId)
                    .get()
                    .await()

                _isLoading.value = false

                if (!documents.isEmpty) {
                    val workspaceList = ArrayList<DrawerMenuItem>()
                    documents.forEach { document ->

                        val workspace = DrawerMenuItem(
                            id = document.id,
                            joinCode = document.getString("joinCode") ?: "",
                            title = document.getString("workspaceName") ?: "",
                            workspaceType = document.getString("workspaceType") ?: ""
                        )
                        workspaceList.add(workspace)
                    }
                    _workspaceList.value = workspaceList
                } else {
                    _workspaceList.value = ArrayList()
                }

            } catch (e: Exception) {
                _isLoading.value = false
                delay(1200)
                _toastMessage.value =
                    "Error retrieving workspaces: ${e.localizedMessage}"
                _workspaceList.value = ArrayList()
            }
        }

    }


}