package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.viewmodel.AddReminderOtherViewModel.UiEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditReminderOtherViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    sealed class UiEvent {
        object ReminderUpdated : UiEvent()
        object ReminderDeleted : UiEvent()
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateHome : UiEvent()
        data class ReminderInformations(
            val title: String,
            val description: String,
            val priority: String,
            val selectedDate: String,
            val selectedTime: String
        ) : UiEvent()

        data class WorkspaceInformations(
            val workspaceName: String,
            val workspaceType: String
        ) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val currentUser = auth.currentUser

    fun deleteReminder(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)

            try {

                firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("reminders")
                    .document(reminderId)
                    .delete()
                    .await()


                _uiEvent.emit(UiEvent.ReminderDeleted)


            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be deleted"))

            }


        }
    }

    fun loadWorkspaceData(workspaceId: String?) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null) {

                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)


            try {
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .get()
                    .await()



                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(
                    UiEvent.WorkspaceInformations(
                        workspaceName = doc.getString("workspaceName") ?: "",
                        workspaceType = doc.getString("workspaceType") ?: "",
                    )
                )

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
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


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)

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

                _uiEvent.emit(UiEvent.ReminderUpdated)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be updated! Please try again."))

            }


        }
    }

    fun loadReminderData(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser == null || workspaceId == null || reminderId == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)


            try {
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("reminders")
                    .document(reminderId)
                    .get()
                    .await()

                if (doc != null && doc.exists()) {

                    _uiEvent.emit(
                        UiEvent.ReminderInformations(
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            priority = doc.getString("priority") ?: "",
                            selectedDate = doc.getString("date") ?: "",
                            selectedTime = doc.getString("time") ?: ""
                        )
                    )
                    _uiEvent.emit(UiEvent.HideLoading)
                }

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error: $e"))

            }


        }
    }

}