package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chnkcksk.reminderapp.model.DrawerMenuItem
import com.chnkcksk.reminderapp.model.Reminder
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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



    fun getUserProviderData(){

        val currentUser = auth.currentUser

        if (currentUser==null){
            _toastMessage.value = "Current user is null"
            return
        }

        currentUser.providerData.forEach { profile ->
            if (profile.providerId == "google.com"){
                _isGoogleUser.value = true
            }
        }

    }

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

        }

    }

    // HomeViewModel.kt - loadWorkspaces() fonksiyonuna debug ekleyin

    fun loadWorkspaces() {
        val currentUser = auth.currentUser
        android.util.Log.d("HomeViewModel", "loadWorkspaces called")

        if (currentUser != null) {
            val userId = currentUser.uid

            _isLoading.value = true

            android.util.Log.d("HomeViewModel", "User ID: $userId")

            firestore.collection("workspaces")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener { documents ->

                    _isLoading.value = false

                    android.util.Log.d(
                        "HomeViewModel",
                        "Firebase query successful, document count: ${documents.size()}"
                    )

                    if (!documents.isEmpty) {
                        val workspaceList = ArrayList<DrawerMenuItem>()
                        documents.forEach { document ->
                            android.util.Log.d("HomeViewModel", "Document ID: ${document.id}")
                            android.util.Log.d(
                                "HomeViewModel",
                                "Workspace Name: ${document.getString("workspaceName")}"
                            )
                            android.util.Log.d(
                                "HomeViewModel",
                                "Workspace Type: ${document.getString("workspaceType")}"
                            )

                            val workspace = DrawerMenuItem(
                                id = document.id,
                                joinCode = document.getString("joinCode") ?: "",
                                title = document.getString("workspaceName") ?: "",
                                workspaceType = document.getString("workspaceType") ?: ""
                            )
                            workspaceList.add(workspace)
                        }
                        android.util.Log.d(
                            "HomeViewModel",
                            "Final workspace list size: ${workspaceList.size}"
                        )
                        _workspaceList.value = workspaceList
                    } else {
                        android.util.Log.d("HomeViewModel", "No workspaces found")
                        _workspaceList.value = ArrayList()
                    }
                }
                .addOnFailureListener { exception ->

                    _isLoading.value = false

                    android.util.Log.e(
                        "HomeViewModel",
                        "Error loading workspaces: ${exception.localizedMessage}"
                    )
                    _toastMessage.value =
                        "Workspace'leri alırken hata oluştu: ${exception.localizedMessage}"
                    _workspaceList.value = ArrayList()
                }
        } else {
            android.util.Log.e("HomeViewModel", "Current user is null")
        }
    }


}