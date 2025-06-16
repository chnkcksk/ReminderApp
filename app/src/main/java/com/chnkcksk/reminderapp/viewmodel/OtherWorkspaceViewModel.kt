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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OtherWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateHome : UiEvent()

        data class WorkspaceInformations(
            val workspaceName: String,
            val editableType: String,
            val ownerId: String
        ) : UiEvent()

        data class ReminderList(val reminderList: ArrayList<Reminder>) : UiEvent()

    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun loadWorkspaceData(workspaceId: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("User not logged in"))
                return@launch
            }
            val userId = currentUser.uid


            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .get()
                    .await()

                val members = doc.get("members") as? List<String> ?: emptyList()

                // Eğer kullanıcı members listesinde yoksa yönlendir
                if (!members.contains(userId)) {
                    _uiEvent.emit(UiEvent.ShowToast("You do not have access to this area"))
                    _uiEvent.emit(UiEvent.NavigateHome)
                    return@launch
                }

                // Kullanıcı üyeyse verileri yükle
                _uiEvent.emit(UiEvent.WorkspaceInformations(
                    ownerId = doc.getString("ownerId") ?: "",
                    editableType = doc.getString("editableType") ?: "",
                    workspaceName = doc.getString("workspaceName") ?: ""
                ))



                _uiEvent.emit(UiEvent.HideLoading)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error fetching workspace: ${e.message}"))
            }


        }
    }


    fun loadRemindersList(workspaceId: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)

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

                _uiEvent.emit(UiEvent.ReminderList(reminderList = reminderList))
                _uiEvent.emit(UiEvent.HideLoading)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast(e.toString()))
            }


        }

    }


}