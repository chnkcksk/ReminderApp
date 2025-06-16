package com.chnkcksk.reminderapp.viewmodel

import android.app.AlertDialog
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object WorkspaceCreated : UiEvent()
        object WorkspaceJoined : UiEvent()

        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        data class WorkspaceInformation(
            val joinCode : String,
            val workspaceId: String
        ):UiEvent()

    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


//    private val _buildDialog = MutableLiveData<Boolean>()
//    val buildDialog: LiveData<Boolean> get() = _buildDialog
//
//    private val _navigateNewWorkspace = MutableLiveData<Boolean>()
//    val navigateNewWorkspace: LiveData<Boolean> get() = _navigateNewWorkspace
//
//    private val _navigateNewWorkspace2 = MutableLiveData<Boolean>()
//    val navigateNewWorkspace2: LiveData<Boolean> get() = _navigateNewWorkspace2


    //lifecyclescope.launch{}

    suspend fun createWorkspace(
        workspaceName: String,
        editableType: String,
        workspaceType: String
    ) {

        val currentUser = auth.currentUser

        if (workspaceName.isEmpty() && editableType.isEmpty() && workspaceType.isEmpty()) {
            _uiEvent.emit(UiEvent.ShowToast("Please fill empty fields"))
            return
        }



        if (currentUser == null) {
            _uiEvent.emit(UiEvent.ShowToast("User not found!"))
            return
        }

        _uiEvent.emit(UiEvent.ShowLoading)

        val userId = currentUser.uid
        val joinCode = generateJoinCode()

        val workspace = hashMapOf(
            "workspaceName" to workspaceName,
            "editableType" to editableType,
            "workspaceType" to workspaceType,
            "ownerId" to userId,
            "members" to listOf(userId),
            "joinCode" to joinCode
        )

        try {
            // Firestore işlemini coroutine ile yapıyoruz
            val documentReference = firestore.collection("workspaces")
                .add(workspace)
                .await()

            _uiEvent.emit(UiEvent.WorkspaceInformation(
                workspaceId = documentReference.id,
                joinCode = joinCode
            ))

            // Başarılı durumda


            _uiEvent.emit(UiEvent.WorkspaceCreated)

        } catch (e: Exception) {
            // Hata durumunda
            _uiEvent.emit(UiEvent.HideLoading)
            _uiEvent.emit(UiEvent.ShowToast("Error creating workspace: ${e.localizedMessage}"))

        }


    }

    suspend fun joinWorkspace(joinCode: String) {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            _uiEvent.emit(UiEvent.ShowToast("Error"))
            return
        }
        val userId = currentUser.uid

        _uiEvent.emit(UiEvent.ShowLoading)

        try {
            // Workspace'i join code ile bul
            val querySnapshot = firestore.collection("workspaces")
                .whereEqualTo("joinCode", joinCode)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0]
                val workspaceId = document.id
                val workspaceType = document.getString("workspaceType") ?: ""

                // Personal workspace kontrolü
                if (workspaceType == "Personal") {
                    _uiEvent.emit(UiEvent.HideLoading)
                    _uiEvent.emit(UiEvent.ShowToast("You cannot join a personal workspace"))

                    return
                }

                _uiEvent.emit(UiEvent.WorkspaceInformation(
                    workspaceId = document.id,
                    joinCode = ""
                ))

                //_workspaceId.value = document.id
                val members = document.get("members") as? List<String> ?: listOf()

                // Kullanıcı zaten üye mi kontrolü
                if (members.contains(userId)) {

                    _uiEvent.emit(UiEvent.HideLoading)
                    _uiEvent.emit(UiEvent.ShowToast("You are already a member of this workspace"))
                    return
                }

                // Üye listesini güncelle
                val updatedMembers = members + userId

                firestore.collection("workspaces")
                    .document(workspaceId)
                    .update("members", updatedMembers)
                    .await()


                _uiEvent.emit(UiEvent.WorkspaceJoined)

                // Başarılı durumda
                //_joinCode.value = joinCode

            } else {

                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("No workspace found with this join code"))

            }

        } catch (e: Exception) {
            _uiEvent.emit(UiEvent.HideLoading)
            _uiEvent.emit(UiEvent.ShowToast("Error joining workspace: ${e.localizedMessage}"))

        }


    }

    fun generateJoinCode(length: Int = 6): String {
        val allowedChars = "ABCDEFGHJKMNPQRSTUVWXYZ123456789" // I, L, O, 0 yok
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")

    }


}