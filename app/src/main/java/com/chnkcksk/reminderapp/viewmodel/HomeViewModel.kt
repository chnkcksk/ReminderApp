package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.checkerframework.checker.units.qual.A
import java.text.SimpleDateFormat
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object GoogleUser : UiEvent()
        data class ReminderList(val reminderList: ArrayList<Reminder>) : UiEvent()
        data class WorkspaceList(val workspaceList: ArrayList<DrawerMenuItem>) : UiEvent()

    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


//    private val _reminderList = MutableLiveData<ArrayList<Reminder>>()
//    val reminderList: LiveData<ArrayList<Reminder>> get() = _reminderList
//
//    private val _workspaceList = MutableLiveData<ArrayList<DrawerMenuItem>>()
//    val workspaceList: LiveData<ArrayList<DrawerMenuItem>> get() = _workspaceList


    fun getUserProviderData() {


        viewModelScope.launch {

            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Current user is null"))
                return@launch
            }

            currentUser.providerData.forEach { profile ->
                if (profile.providerId == "google.com") {
                    //Bu veri gonderilince fragmentta googleuser degeri true olacak
                    _uiEvent.emit(UiEvent.GoogleUser)
                }
            }
        }


    }


    fun loadRemindersList() {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            _uiEvent.emit(UiEvent.ShowLoading)

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
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

                    val reminderValue = document.getBoolean("reminder")
                    Log.d("ReminderCheck", "Reminder ID: ${document.id} - reminder: $reminderValue")


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

                _uiEvent.emit(
                    UiEvent.ReminderList(
                        reminderList = reminderList
                    )
                )
                _uiEvent.emit(UiEvent.HideLoading)

            } catch (e: Exception) {

                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Failed to load reminders: $e"))
                Log.d("Home", "Failed to load reminders: $e")

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



    fun loadWorkspaces() {

        viewModelScope.launch {


            val currentUser = auth.currentUser
            android.util.Log.d("HomeViewModel", "loadWorkspaces called")

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch
            }

            val userId = currentUser.uid

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val documents = firestore.collection("workspaces")
                    .whereArrayContains("members", userId)
                    .get()
                    .await()

                _uiEvent.emit(UiEvent.HideLoading)

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
                    _uiEvent.emit(UiEvent.WorkspaceList(workspaceList = workspaceList))
                } else {
                    _uiEvent.emit(UiEvent.WorkspaceList(workspaceList = ArrayList()))
                }

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error retrieving workspaces: ${e.localizedMessage}"))
                _uiEvent.emit(UiEvent.WorkspaceList(workspaceList = ArrayList()))
            }
        }

    }


}