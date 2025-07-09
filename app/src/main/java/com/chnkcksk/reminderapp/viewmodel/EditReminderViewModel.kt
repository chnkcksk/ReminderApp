package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditReminderViewModel(application: Application) : AndroidViewModel(application) {


    sealed class UiEvent {
        object ReminderEdited : UiEvent()
        object ReminderDeleted : UiEvent()
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateHome : UiEvent()
        object SetNotification : UiEvent()
        data class ReminderInformations(
            val title: String = "",
            val description: String = "",
            val priority: String = "None",
            val selectedDate: String = "",
            val selectedTime: String = "",
            val reminderState: Boolean = false
        ) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val currentUser = auth.currentUser

    fun deleteReminder(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
                return@launch
            }


            val userId = currentUser.uid

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                firestore.collection("Users").document(userId).collection("workspaces")
                    .document(workspaceId).collection("reminders").document(reminderId).delete()
                    .await()

                _uiEvent.emit(UiEvent.ReminderDeleted)


            } catch (e: Exception) {

                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be deleted: ${e.message}"))

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
        time: String,
        isNotificationChecked: Boolean

    ) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
                return@launch
            }


            val userId = currentUser.uid

            _uiEvent.emit(UiEvent.ShowLoading)


            val updatedData = hashMapOf<String, Any>(
                "title" to title,
                "description" to description,
                "lasttimestamp" to System.currentTimeMillis(),
                "priority" to priority,
                "date" to date,
                "time" to time,
                "reminder" to isNotificationChecked
            )

            try {
                firestore.collection("Users").document(userId).collection("workspaces")
                    .document(workspaceId).collection("reminders").document(reminderId)
                    .update(updatedData).await()


                //kutucuk seciliyse bildirim ayarla
                if (isNotificationChecked == true) {
                    _uiEvent.emit(UiEvent.SetNotification)
                }

                _uiEvent.emit(UiEvent.ReminderEdited)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error: ${e.message}"))

            }


        }
    }

    fun loadReminderData(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
                return@launch
            }
            val userId = currentUser.uid

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val doc =
                    firestore.collection("Users").document(userId).collection("workspaces")
                        .document(workspaceId).collection("reminders").document(reminderId)
                        .get().await()

                if (doc != null && doc.exists()) {

                    _uiEvent.emit(UiEvent.HideLoading)

                    _uiEvent.emit(
                        UiEvent.ReminderInformations(
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            priority = doc.getString("priority") ?: "",
                            selectedDate = doc.getString("date") ?: "",
                            selectedTime = doc.getString("time") ?: "",
                            reminderState = doc.getBoolean("reminder") ?: false
                        )
                    )


                }

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error: ${e.message}"))

            }


        }
    }

}