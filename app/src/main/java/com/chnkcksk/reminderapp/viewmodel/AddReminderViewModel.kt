package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore


    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateHome : UiEvent()
        object SetNotification : UiEvent()
        object ReminderAdded:UiEvent()
    }


    fun addReminder(
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String,
        isNotificationChecked: Boolean

    ) {

        viewModelScope.launch {

            _uiEvent.emit(UiEvent.ShowLoading)

            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("User login required"))
                return@launch
            }

            val reminder = hashMapOf(
                "title" to title,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "isCompleted" to false,
                "priority" to priority,
                "date" to date,
                "time" to time,
                "reminder" to isNotificationChecked
            )

            try {

                firestore.collection("Users")
                    .document(currentUser.uid)
                    .collection("workspaces")
                    .document("personalWorkspace")
                    .collection("reminders")
                    .add(reminder)
                    .await()


                if (isNotificationChecked) {
                    _uiEvent.emit(UiEvent.SetNotification)
                }

                _uiEvent.emit(UiEvent.ReminderAdded)


            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be saved!"))
            }

        }

    }

}