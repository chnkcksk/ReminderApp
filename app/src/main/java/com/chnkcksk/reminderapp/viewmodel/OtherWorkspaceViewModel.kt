package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OtherWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _editableType = MutableLiveData<String>()
    val editableType: LiveData<String> get() = _editableType

    private val _ownerId = MutableLiveData<String>()
    val ownerId: LiveData<String> get() = _ownerId

    private val _reminderList = MutableLiveData<ArrayList<Reminder>>()
    val reminderList: LiveData<ArrayList<Reminder>> get() = _reminderList

    fun loadWorkspaceData(workspaceId: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser != null) {
                val userId = currentUser.uid

                _isLoading.value = true

                try {
                    val doc = firestore.collection("workspaces")
                        .document(workspaceId)
                        .get()
                        .await()

                    val members = doc.get("members") as? List<String> ?: emptyList()

                    // Eğer kullanıcı members listesinde yoksa yönlendir
                    if (!members.contains(userId)) {
                        _toastMessage.value = "You do not have access to this area"
                        delay(500)
                        _navigateHome.value = true
                        return@launch
                    }

                    // Kullanıcı üyeyse verileri yükle
                    _ownerId.value = doc.getString("ownerId") ?: ""
                    _editableType.value = doc.getString("editableType") ?: ""
                    _workspaceName.value = doc.getString("workspaceName") ?: ""

                    _isLoading.value = false

                } catch (e: Exception) {
                    _isLoading.value = false
                    Log.e("Workspace", "Error fetching workspace: ${e.message}", e)
                }


            } else {
                Log.d("Workspace", "User not logged in")
            }
        }
    }


    fun loadRemindersList(workspaceId: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _toastMessage.value = "Error"
                return@launch
            }

            _isLoading.value = true

            try {
                val documents = firestore.collection("workspaces")
                    .document(workspaceId)
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
                        time = document.getString("time") ?: ""
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
                _toastMessage.value = e.toString()
            }


        }

    }


}