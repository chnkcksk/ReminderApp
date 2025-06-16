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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PasswordChangeViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object PasswordChanged : UiEvent()

        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        object NavigateHome : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun reAuthenticateAndChangePassword(oldPassw: String, newPassw: String, newPasswAgain: String) {

        viewModelScope.launch {


            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("User not found!"))
                return@launch
            }

            if (oldPassw.isEmpty() || newPassw.isEmpty() || newPasswAgain.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowToast("Please fill blank fields!"))
                return@launch
            }


            if (!newPassw.equals(newPasswAgain)) {
                _uiEvent.emit(UiEvent.ShowToast("Passwords are not the same!"))
                return@launch
            }

            if (newPassw.equals(oldPassw)) {
                _uiEvent.emit(UiEvent.ShowToast("The old password cannot be the same as the new password!"))
                return@launch
            }

            val userId = currentUser.uid
            val email = currentUser.email

            if (email == null) {
                _uiEvent.emit(UiEvent.ShowToast("Email not found!"))
                return@launch
            }

            _uiEvent.emit(UiEvent.ShowLoading)


            try {

                val credential = EmailAuthProvider.getCredential(email, oldPassw)

                // Re-authenticate the user
                currentUser.reauthenticate(credential).await()

                // Update the password
                currentUser.updatePassword(newPassw).await()


                //PasswordChanged
                _uiEvent.emit(UiEvent.PasswordChanged)


            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(
                    UiEvent.ShowToast(
                        e.localizedMessage ?: "An unexpected error occurred."
                    )
                )


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