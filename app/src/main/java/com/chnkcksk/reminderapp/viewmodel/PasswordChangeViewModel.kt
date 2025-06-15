package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PasswordChangeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateHome = MutableLiveData<Boolean>()
    val navigateHome: LiveData<Boolean> get() = _navigateHome

    private val _clearFields = MutableLiveData<Boolean>()
    val clearFields: LiveData<Boolean> get() = _clearFields

    private val _viewSuccessAnim = MutableLiveData<Boolean>()
    val viewSuccessAnim: LiveData<Boolean> get() = _viewSuccessAnim

    fun reAuthenticateAndChangePassword(oldPassw: String, newPassw: String, newPasswAgain: String) {

        viewModelScope.launch {



        val currentUser = auth.currentUser

        if (currentUser == null) {
            _toastMessage.value = "User not found!"
            return@launch
        }

        if (oldPassw.isEmpty() || newPassw.isEmpty() || newPasswAgain.isEmpty()) {
            _toastMessage.value = "Please fill blank fields!"
            return@launch
        }


        if (!newPassw.equals(newPasswAgain)) {
            _toastMessage.value = "Passwords are not the same!"
            return@launch
        }

        if (newPassw.equals(oldPassw)){
            _toastMessage.value = "The old password cannot be the same as the new password!"
            return@launch
        }

        val userId = currentUser.uid
        val email = currentUser.email

        if (email == null) {
            _toastMessage.value = "Email not found!"
            return@launch
        }

        _isLoading.value = true


        try {

            val credential = EmailAuthProvider.getCredential(email, oldPassw)

            // Re-authenticate the user
            currentUser.reauthenticate(credential).await()

            // Update the password
            currentUser.updatePassword(newPassw).await()

            _isLoading.value = false
            delay(1200)
            _viewSuccessAnim.value = true
            delay(2000)
            _clearFields.value = true




        }catch (e:Exception){
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = e.localizedMessage ?: "An unexpected error occurred."
        }



        /*
        currentUser.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful){
                    currentUser.updatePassword(newPassw)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful){
                                _isLoading.value = false
                                _toastMessage.value = "Password changed successfully"
                                _viewSuccessAnim.value = true
                                _clearFields.value = true
                            }else{
                                _isLoading.value = false
                                _toastMessage.value = "Failed to change password: ${updateTask.exception?.message}"
                            }
                        }
                }else{
                    _isLoading.value = false
                    _toastMessage.value = "Re-authentication failed: ${authTask.exception?.message}"
                }
            }

         */
        }

    }

    fun togglePasswordVisibility(editText: EditText, toggleButton: ImageButton) {
        if (editText.transformationMethod is PasswordTransformationMethod) {
            // Şifreyi göster
            editText.transformationMethod = null
            toggleButton.setImageResource(R.drawable.baseline_visibility_24)
        } else {
            // Şifreyi gizle
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.baseline_visibility_off_24)
        }

        // İmleci metnin sonuna taşı
        editText.setSelection(editText.text.length)
    }

}