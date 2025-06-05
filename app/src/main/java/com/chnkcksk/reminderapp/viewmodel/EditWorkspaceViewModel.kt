package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditWorkspaceViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateWorkspace = MutableLiveData<Boolean>()
    val navigateWorkspace: LiveData<Boolean> get() = _navigateWorkspace

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome: LiveData<Boolean> get() = _navigateHome

    private val _workspaceName = MutableLiveData<String>()
    val workspaceName: LiveData<String> get() = _workspaceName

    private val _workspaceType = MutableLiveData<String>()
    val workspaceType: LiveData<String> get() = _workspaceType

    private val _workspaceCode = MutableLiveData<String>()
    val workspaceCode: LiveData<String> get() = _workspaceCode

    private val _ownerId = MutableLiveData<String>()
    val ownerId: LiveData<String> get() = _ownerId

    private val _editableType = MutableLiveData<String>()
    val editableType: LiveData<String> get() = _editableType

    private val _memberNames = MutableLiveData<List<String>>()
    val memberNames: LiveData<List<String>> get() = _memberNames

    fun fetchWorkspaceMemberNames(workspaceId: String) {
        _isLoading.value = true

        firestore.collection("workspaces")
            .document(workspaceId)
            .get()
            .addOnSuccessListener { doc ->
                val memberIds = doc.get("members") as? List<String> ?: emptyList()
                val memberNamesList = mutableListOf<String>()

                if (memberIds.isEmpty()) {
                    _memberNames.value = memberNamesList
                    _isLoading.value = false
                    return@addOnSuccessListener
                }

                var fetchedCount = 0

                for (id in memberIds) {
                    firestore.collection("Users")
                        .document(id)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("name") ?: "Unknown"
                            memberNamesList.add(username)
                            fetchedCount++

                            if (fetchedCount == memberIds.size) {
                                _memberNames.value = memberNamesList
                                _isLoading.value = false
                            }
                        }
                        .addOnFailureListener {
                            memberNamesList.add("Unknown")
                            fetchedCount++

                            if (fetchedCount == memberIds.size) {
                                _memberNames.value = memberNamesList
                                _isLoading.value = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                _toastMessage.value = "Could not load workspace members."
                _isLoading.value = false
            }
    }


    fun quitWorkspace(workspaceId: String) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            _isLoading.value = true

            firestore.collection("workspaces")
                .document(workspaceId)
                .update("members", FieldValue.arrayRemove(userId))
                .addOnSuccessListener {
                    _toastMessage.value = "Successfully left the workspace."
                    _isLoading.value = false
                    _navigateHome.value = true
                }
                .addOnFailureListener { e ->
                    _toastMessage.value = "Failed to leave workspace: ${e.localizedMessage}"
                    _isLoading.value = false
                }
        } else {
            _toastMessage.value = "User not found!"
            _isLoading.value = false
        }
    }


    fun getWorkspaceData(workspaceId: String) {
        val currentUser = auth.currentUser

        if (currentUser != null && workspaceId.isNotEmpty()) {
            val userId = currentUser.uid

            _isLoading.value = true

            firestore.collection("workspaces")
                .document(workspaceId)
                .get()
                .addOnSuccessListener { doc ->

                    val members = doc.get("members") as? List<String> ?: emptyList()

                    // Eğer kullanıcı members listesinde yoksa yönlendir
                    if (!members.contains(userId)) {
                        _toastMessage.value = "You do not have access to this area"
                        _navigateHome.value = true
                        return@addOnSuccessListener
                    }

                    _isLoading.value = false
                    _workspaceName.value = doc.getString("workspaceName") ?: ""
                    _workspaceType.value = doc.getString("workspaceType") ?: ""
                    _editableType.value = doc.getString("editableType") ?:""
                    _workspaceCode.value = doc.getString("joinCode") ?: ""
                    _ownerId.value = doc.getString("ownerId") ?: ""
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    Log.e("Workspace", "Error fetching workspace: ${e.message}", e)
                }

        } else {
            Log.d("Workspace", "User not logged in")
        }
    }

    fun deleteWorkspace(workspaceId: String){
        val currentUser = auth.currentUser

        if (currentUser==null){
            return
            _toastMessage.value = "Error"
        }

        _isLoading.value = true

        firestore.collection("workspaces")
            .document(workspaceId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                _toastMessage.value = "Workspace successfully deleted"
                _navigateWorkspace.value = true
            }
            .addOnFailureListener {
                _isLoading.value = false
                _toastMessage.value = "Workspace could not be deleted"
            }


    }

    fun editWorkspace(workspaceId: String, editedWorkspaceName: String, wT:String, eT:String, kickOthers:Boolean){
        val currentUser = auth.currentUser

        if (currentUser==null){
            return
            _toastMessage.value = "Error"
        }

        _isLoading.value = true



        if (kickOthers == true) {
            // Önce mevcut üyeleri çekiyoruz
            firestore.collection("workspaces")
                .document(workspaceId)
                .get()
                .addOnSuccessListener { document ->
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
                        .addOnSuccessListener {
                            _toastMessage.value = "Workspace edited successfully"
                            _isLoading.value = false
                            _navigateWorkspace.value = true
                        }
                        .addOnFailureListener {
                            _toastMessage.value = "Error"
                            _isLoading.value = false
                        }
                }
                .addOnFailureListener {
                    _toastMessage.value = "Failed to fetch current members"
                    _isLoading.value = false
                }
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
                .addOnSuccessListener {
                    _toastMessage.value = "Workspace edited successfully"
                    _isLoading.value = false
                    _navigateWorkspace.value = true
                }
                .addOnFailureListener {
                    _toastMessage.value = "Error"
                    _isLoading.value = false
                }
        }




    }

}