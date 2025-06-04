package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chnkcksk.reminderapp.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _title = MutableLiveData<String>()
    val title : LiveData<String> get() = _title

    private val _description = MutableLiveData<String>()
    val description : LiveData<String> get() = _description

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome : LiveData<Boolean> get() = _navigateHome

    //priority,date,time

    private val _priority = MutableLiveData<String>()
    val priority : LiveData<String> get() = _priority

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate : LiveData<String> get() = _selectedDate

    private val _selectedTime = MutableLiveData<String>()
    val selectedTime : LiveData<String> get() = _selectedTime


    private val currentUser = auth.currentUser

    fun deleteReminder(workspaceId:String?, reminderId:String?){
        if (currentUser!=null && workspaceId!=null && reminderId!= null){

            val userId = currentUser.uid

            _isLoading.value = true

            firestore.collection("Users")
                .document(userId)
                .collection("workspaces")
                .document(workspaceId)
                .collection("reminders")
                .document(reminderId)
                .delete()
                .addOnSuccessListener {
                    _navigateHome.value = true
                    _toastMessage.value = "Reminder deleted"
                    _isLoading.value = false
                }
                .addOnFailureListener {
                    _toastMessage.value = "Reminder could not be deleted"
                    _isLoading.value = false
                }


        }else{
            _toastMessage.value = "Error!"
            _isLoading.value = false
        }
    }

    fun editReminderData(workspaceId:String?, reminderId:String?){

    }

    fun loadReminderData(workspaceId:String?, reminderId:String?){

        _isLoading.value = true

        if (currentUser!=null && workspaceId!=null && reminderId!=null){
            val userId = currentUser.uid

            firestore.collection("Users")
                .document(userId)
                .collection("workspaces")
                .document(workspaceId)
                .collection("reminders")
                .document(reminderId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc!=null && doc.exists()){

                        //priority,date,time
                        _title.value = doc.getString("title")
                        _description.value = doc.getString("description")
                        _priority.value = doc.getString("priority")
                        _selectedDate.value = doc.getString("date")
                        _selectedTime.value = doc.getString("time")

                        _isLoading.value = false
                    }
                }.addOnFailureListener { e->
                    _toastMessage.value = "Error: $e"
                    _isLoading.value = false
                }

        }else{
            _toastMessage.value = "Error!"
            _isLoading.value = false
        }

    }

}