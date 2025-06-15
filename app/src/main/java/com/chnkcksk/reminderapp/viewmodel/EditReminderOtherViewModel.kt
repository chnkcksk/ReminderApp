package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditReminderOtherViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> get() = _description

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome: LiveData<Boolean> get() = _navigateHome

    //priority,date,time

    private val _priority = MutableLiveData<String>()
    val priority: LiveData<String> get() = _priority

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> get() = _selectedDate

    private val _selectedTime = MutableLiveData<String>()
    val selectedTime: LiveData<String> get() = _selectedTime

    private val _workspaceName = MutableLiveData<String>()
    val workspaceName: LiveData<String> get() = _workspaceName

    private val _workspaceType = MutableLiveData<String>()
    val workspaceType: LiveData<String> get() = _workspaceType


    private val currentUser = auth.currentUser

    fun deleteReminder(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser != null && workspaceId != null && reminderId != null) {

                _isLoading.value = true

                try {

                    firestore.collection("workspaces")
                        .document(workspaceId)
                        .collection("reminders")
                        .document(reminderId)
                        .delete()
                        .await()

                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Reminder deleted"
                    delay(500)
                    _navigateHome.value = true


                } catch (e: Exception) {
                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Reminder could not be deleted"

                }


            } else {
                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "Error!"

            }
        }
    }

    fun loadWorkspaceData(workspaceId: String?) {

        viewModelScope.launch {


            if (currentUser == null) {
                return@launch
            }

            _isLoading.value = true



            if (workspaceId != null) {

                try {
                    val doc = firestore.collection("workspaces")
                        .document(workspaceId)
                        .get()
                        .await()

                    _isLoading.value = false
                    _workspaceName.value = doc.getString("workspaceName") ?: ""
                    _workspaceType.value = doc.getString("workspaceType") ?: ""

                } catch (e: Exception) {
                    _isLoading.value = false
                }


            }
        }


    }

    fun editReminderData(
        workspaceId: String?,
        reminderId: String?,
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String
    ) {

        viewModelScope.launch {


            _isLoading.value = true

            if (currentUser != null && workspaceId != null && reminderId != null) {

                val userId = currentUser.uid

                val updatedData = hashMapOf<String, Any>(
                    "title" to title,
                    "description" to description,
                    "priority" to priority,
                    "date" to date,
                    "time" to time,
                )
                try {
                    firestore
                        .collection("workspaces").document(workspaceId)
                        .collection("reminders").document(reminderId)
                        .update(updatedData)
                        .await()

                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Reminder updated!"
                    delay(500)
                    _navigateHome.value = true
                } catch (e: Exception) {
                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Reminder could not be updated! Please try again."

                }


            } else {
                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "Error."

            }
        }
    }

    fun loadReminderData(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            _isLoading.value = true

            if (currentUser != null && workspaceId != null && reminderId != null) {

                try {
                    val doc = firestore.collection("workspaces")
                        .document(workspaceId)
                        .collection("reminders")
                        .document(reminderId)
                        .get()
                        .await()

                    if (doc != null && doc.exists()) {

                        //priority,date,time
                        _title.value = doc.getString("title")
                        _description.value = doc.getString("description")
                        _priority.value = doc.getString("priority")
                        _selectedDate.value = doc.getString("date")
                        _selectedTime.value = doc.getString("time")

                        _isLoading.value = false
                    }

                } catch (e: Exception) {
                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Error: $e"

                }


            } else {
                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "Error!"

            }
        }
    }

}