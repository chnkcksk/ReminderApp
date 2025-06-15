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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditReminderViewModel(application: Application) : AndroidViewModel(application) {

    //UI State Data Class + StateFlow/LiveData


    data class UiState(
        val reminderDeleted:Boolean = false,
        val reminderNotDeleted:Boolean = false,
        val reminderEdited:Boolean = false,
        val reminderNotEdited:Boolean = false,
        val isLoading: Boolean = false,
        val toastMessage: String? = null,
        val title: String = "",
        val description: String = "",
        val priority: String = "None",
        val selectedDate: String = "",
        val selectedTime: String = "",
        val reminderState: Boolean = false,
        val navigateHome: Boolean = false,
        val setNotification: Boolean = false,
        val error: String? = null
    )

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val currentUser = auth.currentUser

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    private fun updateUiState(update: UiState.() -> UiState) {
        _uiState.value = _uiState.value.update()
    }

    fun deleteReminder(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser != null && workspaceId != null && reminderId != null) {

                val userId = currentUser.uid

                updateUiState { copy(isLoading = true) }

                try {
                    firestore.collection("Users").document(userId).collection("workspaces")
                        .document(workspaceId).collection("reminders").document(reminderId).delete()
                        .await()

                    updateUiState {
                        copy(
                            reminderDeleted = true
                        )
                    }


                } catch (e: Exception) {

                    updateUiState {
                        copy(
                            reminderNotDeleted = true,
                            error = e.message
                        )
                    }

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
        time: String,
        isNotificationChecked: Boolean

    ) {

        viewModelScope.launch {


            if (currentUser != null && workspaceId != null && reminderId != null) {

                val userId = currentUser.uid

                updateUiState {
                    copy(isLoading = true)
                }


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
                        updateUiState { copy(setNotification = true) }
                    }

                    updateUiState {
                        copy(
                            reminderEdited = true
                        )
                    }

                } catch (e: Exception) {
                    updateUiState {
                        copy(
                            reminderNotEdited = true,
                            error = e.message
                        )
                    }

                }


            }
        }
    }

    fun loadReminderData(workspaceId: String?, reminderId: String?) {

        viewModelScope.launch {


            if (currentUser != null && workspaceId != null && reminderId != null) {
                val userId = currentUser.uid

                updateUiState {
                    copy(isLoading = true)
                }

                try {
                    val doc =
                        firestore.collection("Users").document(userId).collection("workspaces")
                            .document(workspaceId).collection("reminders").document(reminderId)
                            .get().await()

                    if (doc != null && doc.exists()) {

                        updateUiState {
                            copy(
                                isLoading = false,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                priority = doc.getString("priority") ?: "",
                                selectedDate = doc.getString("date") ?: "",
                                selectedTime = doc.getString("time") ?: "",
                                reminderState = doc.getBoolean("reminder") ?: false

                            )
                        }

                    }

                } catch (e: Exception) {
                    updateUiState {
                        copy(
                            isLoading = false,
                            toastMessage = "Error: ${e.message}",
                            error = e.message
                        )
                    }
                }


            } else {
                updateUiState {
                    copy(
                        isLoading = false,
                        toastMessage = "Error!",
                        error = "Invalid parameters"
                    )
                }
            }
        }
    }


    // Event'leri temizle
    fun clearToastMessage() {
        updateUiState { copy(toastMessage = null) }
    }

    fun clearNavigateHome() {
        updateUiState { copy(navigateHome = false) }
    }

    fun clearSetNotification() {
        updateUiState { copy(setNotification = false) }
    }
}