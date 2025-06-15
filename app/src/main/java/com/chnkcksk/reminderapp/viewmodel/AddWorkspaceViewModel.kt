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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome: LiveData<Boolean> get() = _navigateHome

    private val _buildDialog = MutableLiveData<Boolean>()
    val buildDialog: LiveData<Boolean> get() = _buildDialog

    private val _joinCode = MutableLiveData<String>()
    val joinCode: LiveData<String> get() = _joinCode

    private val _workspaceId = MutableLiveData<String>()
    val workspaceId: LiveData<String> get() = _workspaceId

    private val _navigateNewWorkspace = MutableLiveData<Boolean>()
    val navigateNewWorkspace: LiveData<Boolean> get() = _navigateNewWorkspace

    private val _navigateNewWorkspace2 = MutableLiveData<Boolean>()
    val navigateNewWorkspace2: LiveData<Boolean> get() = _navigateNewWorkspace2

    private val _viewSuccessDialog = MutableLiveData<Boolean>()
    val viewSuccessDialog: LiveData<Boolean> get() = _viewSuccessDialog


    //lifecyclescope.launch{}

    suspend fun createWorkspace(
        workspaceName: String,
        editableType: String,
        workspaceType: String
    ) {

        val currentUser = auth.currentUser

        if (workspaceName.isEmpty() && editableType.isEmpty() && workspaceType.isEmpty()) {
            _toastMessage.value = "Please fill empty fields"
            return
        }

        _isLoading.value = true

        if (currentUser != null) {
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

                // Başarılı durumda
                _workspaceId.value = documentReference.id
                _joinCode.value = joinCode
                //_isLoading.value = false
                _viewSuccessDialog.value = true
                delay(2000)
                _buildDialog.value = true
                _navigateNewWorkspace.value = true

            } catch (e: Exception) {
                // Hata durumunda
                //_isLoading.value = false
                delay(1200)
                _toastMessage.value = "Error creating workspace: ${e.localizedMessage}"

            }finally {
                _isLoading.value = false
            }
        } else {
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = "User not found!"
        }


    }

    suspend fun joinWorkspace(joinCode: String) {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            _toastMessage.value = "Error"
            return
        }
        val userId = currentUser.uid
        _isLoading.value = true

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
                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "You cannot join a personal workspace"

                    return
                }

                _workspaceId.value = document.id
                val members = document.get("members") as? List<String> ?: listOf()

                // Kullanıcı zaten üye mi kontrolü
                if (members.contains(userId)) {

                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "You are already a member of this workspace"
                    return
                }

                // Üye listesini güncelle
                val updatedMembers = members + userId

                firestore.collection("workspaces")
                    .document(workspaceId)
                    .update("members", updatedMembers)
                    .await()

                // Başarılı durumda
                _isLoading.value = false
                delay(1200)
                _viewSuccessDialog.value = true
                delay(2000)
                _joinCode.value = joinCode
                _navigateNewWorkspace2.value = true

            } else {

                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "No workspace found with this join code"
            }

        } catch (e: Exception) {
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = "Error joining workspace: ${e.localizedMessage}"

        }



    }

    fun generateJoinCode(length: Int = 6): String {
        val allowedChars = "ABCDEFGHJKMNPQRSTUVWXYZ123456789" // I, L, O, 0 yok
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")

    }


}