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
    val navigateNewWorkspace : LiveData<Boolean> get() = _navigateNewWorkspace

    private val _navigateNewWorkspace2 = MutableLiveData<Boolean>()
    val navigateNewWorkspace2 : LiveData<Boolean> get() = _navigateNewWorkspace2

    fun createWorkspace(workspaceName: String, editableType: String, workspaceType: String) {

        val currentUser = auth.currentUser

        if (workspaceName.isEmpty() && editableType.isEmpty() && workspaceName.isEmpty()) {
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

            firestore.collection("workspaces")
                .add(workspace)
                .addOnSuccessListener { documentReference ->
                    _workspaceId.value = documentReference.id
                    _joinCode.value = joinCode
                    _toastMessage.value = "Workspace created successfully!"
                    _isLoading.value = false
                    _buildDialog.value = true
                    _navigateNewWorkspace.value = true
                }
                .addOnFailureListener { e ->
                    _toastMessage.value = "Error creating workspace: ${e.localizedMessage}"
                    _isLoading.value = false
                }


        } else {
            _toastMessage.value = "User not found!"
            _isLoading.value = false
        }


    }

    fun joinWorkspace(joinCode:String){

        val currentUser = auth.currentUser

        if (currentUser!=null){
            val userId = currentUser.uid

            _isLoading.value = true

            firestore.collection("workspaces")
                .whereEqualTo("joinCode",joinCode).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val workspaceId = document.id

                        val workspaceType = document.getString("workspaceType")?:""

                        if (workspaceType=="Personal"){
                            _toastMessage.value = "You cannot join a personal workspace"
                            _isLoading.value = false
                            return@addOnSuccessListener
                        }

                        _workspaceId.value = document.id
                        val members = document.get("members") as? List<String> ?: listOf()

                        // Eğer kullanıcı zaten üyeyse tekrar ekleme
                        if (members.contains(userId)) {
                            _toastMessage.value = "You are already a member of this workspace"
                            _isLoading.value = false
                            return@addOnSuccessListener
                        }

                        val updatedMembers = members + userId

                        firestore.collection("workspaces")
                            .document(workspaceId)
                            .update("members", updatedMembers)
                            .addOnSuccessListener {
                                _joinCode.value = joinCode
                                _toastMessage.value = "Successfully joined workspace!"
                                _isLoading.value = false
                                _navigateNewWorkspace2.value = true
                            }
                            .addOnFailureListener { e ->
                                _toastMessage.value = "Failed to join workspace: ${e.localizedMessage}"
                                _isLoading.value = false
                            }
                    } else {
                        _toastMessage.value = "No workspace found with this join code"
                        _isLoading.value = false
                    }
                }
                .addOnFailureListener { e ->
                    _toastMessage.value = "Error joining workspace: ${e.localizedMessage}"
                    _isLoading.value = false
                }

        }else{
            _toastMessage.value = "User not found!"
            _isLoading.value = false
        }





    }

    fun generateJoinCode(length: Int = 6): String {
        val allowedChars = "ABCDEFGHJKMNPQRSTUVWXYZ123456789" // I, L, O, 0 yok
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")

    }


}