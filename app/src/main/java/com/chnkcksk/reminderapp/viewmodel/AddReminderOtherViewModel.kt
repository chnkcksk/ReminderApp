package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AddReminderOtherViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore


    sealed class UiEvent {
        object ReminderAdded : UiEvent()
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateWorkspace : UiEvent()
        data class WorkspaceInformations(
            val workspaceName: String,
            val workspaceType: String,
        ) : UiEvent()
    }


    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun addOtherReminder(
        workspaceId: String,
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String,
        creatorName: String
    ) {
        viewModelScope.launch {

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("User login required"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)

            val reminder = hashMapOf(
                "title" to title,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "isCompleted" to false,
                "priority" to priority,
                "date" to date,
                "time" to time,
                "creatorName" to creatorName
            )

            try {
                // Firestore işlemine await() ile coroutine destekli asenkron işlem
                firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("reminders")
                    .add(reminder)
                    .await()

                _uiEvent.emit(UiEvent.ReminderAdded)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be added"))
            }
        }
    }


    fun getDatas(workspaceId: String) {
        viewModelScope.launch {

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .get()
                    .await()

                _uiEvent.emit(
                    UiEvent.WorkspaceInformations(
                        workspaceName = doc.getString("workspaceName") ?: "",
                        workspaceType = doc.getString("workspaceType") ?: "",
                    )
                )

                _uiEvent.emit(UiEvent.HideLoading)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowToast("${e.message}"))
                _uiEvent.emit(UiEvent.HideLoading)
            }
        }
    }
}
