package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object QuitWorkspace : UiEvent()
        object WorkspaceDeleted : UiEvent()
        object WorkspaceEdited : UiEvent()

        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        object NavigateHome : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        data class MembersList(val members: List<String>) : UiEvent()
        data class WorkspaceInformation(
            val workspaceName: String,
            val workspaceType: String,
            val workspaceCode: String,
            val ownerId: String,
            val editableType: String
        ) : UiEvent()

    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun fetchWorkspaceMemberNames(workspaceId: String) {

        viewModelScope.launch {


            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .get()
                    .await()

                val memberIds = doc.get("members") as? List<String> ?: emptyList()
                val memberNamesList = mutableListOf<String>()

                if (memberIds.isEmpty()) {
                    _uiEvent.emit(
                        UiEvent.MembersList(
                            members = memberNamesList
                        )
                    )
                    _uiEvent.emit(UiEvent.HideLoading)
                    return@launch
                }

                for (id in memberIds) {

                    try {
                        val userDoc = firestore.collection("Users")
                            .document(id)
                            .get()
                            .await()

                        val username = userDoc.getString("name") ?: "Unknown"
                        memberNamesList.add(username)

                    } catch (e: Exception) {
                        memberNamesList.add("Unknown")
                    }

                    _uiEvent.emit(UiEvent.MembersList(members = memberNamesList))
                    _uiEvent.emit(UiEvent.HideLoading)

                }

            } catch (e: Exception) {

                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Could not load workspace members."))
            }

        }
    }


    fun quitWorkspace(workspaceId: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("User not found!"))
                return@launch
            }

            val userId = currentUser.uid

            _uiEvent.emit(UiEvent.ShowLoading)


            try {
                // Firestore işlemini await ile bekle
                firestore.collection("workspaces")
                    .document(workspaceId)
                    .update("members", FieldValue.arrayRemove(userId))
                    .await()

                // Burası ancak işlem başarılı olursa çalışır (addOnSuccessListener yerine)

                // Başarılı olduktan sonra sıralı işlemler


                _uiEvent.emit(UiEvent.QuitWorkspace)

            } catch (e: Exception) {
                // Burası ancak işlem basarisiz olursa çalışır (addOnFailureListener yerine)
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Failed to leave workspace: ${e.localizedMessage}"))
            }


        }
    }


    fun getWorkspaceData(workspaceId: String) {

        viewModelScope.launch {

            val currentUser = auth.currentUser

            if (currentUser == null || workspaceId.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
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
                        _uiEvent.emit(UiEvent.HideLoading)
                        return@launch
                    }

                    _uiEvent.emit(
                        UiEvent.WorkspaceInformation(
                            workspaceName = doc.getString("workspaceName") ?: "",
                            workspaceType = doc.getString("workspaceType") ?: "",
                            editableType = doc.getString("editableType") ?: "",
                            workspaceCode = doc.getString("joinCode") ?: "",
                            ownerId = doc.getString("ownerId") ?: ""
                        )
                    )
                    _uiEvent.emit(UiEvent.HideLoading)

                } catch (e: Exception) {
                    _uiEvent.emit(UiEvent.HideLoading)
                    Log.e("Workspace", "Error fetching workspace: ${e.message}", e)
                }



        }
    }

    fun deleteWorkspace(workspaceId: String) {

        viewModelScope.launch {

            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch

            }

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                firestore.collection("workspaces")
                    .document(workspaceId)
                    .delete()
                    .await()

                _uiEvent.emit(UiEvent.WorkspaceDeleted)

            } catch (e: Exception) {

                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Workspace could not be deleted"))
            }
        }

    }

    fun editWorkspace(
        workspaceId: String,
        editedWorkspaceName: String,
        wT: String,
        eT: String,
        kickOthers: Boolean
    ) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error"))
                return@launch

            }

            _uiEvent.emit(UiEvent.ShowLoading)



            try {

                if (kickOthers == true) {
                    // Önce mevcut üyeleri çekiyoruz
                    val document = firestore.collection("workspaces")
                        .document(workspaceId)
                        .get()
                        .await()

                    val currentMembers = document.get("members") as? List<String> ?: listOf()
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    // Sadece kendi userId'ni içeren yeni members listesi oluştur
                    val newMembers = listOf(currentUserId)

                    val updatedData = hashMapOf<String, Any>(
                        "workspaceName" to editedWorkspaceName,
                        "workspaceType" to wT,
                        "editableType" to eT,
                        "members" to newMembers // diğer üyeleri çıkar
                    )

                    // Firestore'da güncelleme işlemi
                    firestore.collection("workspaces")
                        .document(workspaceId)
                        .update(updatedData)
                        .await()

                    _uiEvent.emit(UiEvent.WorkspaceEdited)


                } else {
                    // Diğer güncelleme (üyeleri değiştirmeden)
                    val updatedData = hashMapOf<String, Any>(
                        "workspaceName" to editedWorkspaceName,
                        "workspaceType" to wT,
                        "editableType" to eT
                    )

                    firestore.collection("workspaces")
                        .document(workspaceId)
                        .update(updatedData)
                        .await()

                    _uiEvent.emit(UiEvent.WorkspaceEdited)
                }

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Error: ${e.localizedMessage}"))


            }


        }

    }

}