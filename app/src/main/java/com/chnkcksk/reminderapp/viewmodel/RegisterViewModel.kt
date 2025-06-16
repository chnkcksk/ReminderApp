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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    sealed class UiEvent {
        object AccountCreated : UiEvent()

        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()

    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    private val auth: FirebaseAuth = Firebase.auth

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {


            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                _uiEvent.emit(UiEvent.ShowToast("Please fill empty fields"))
                return@launch
            }

            val formattedName = name.lowercase().split(" ").joinToString(" ") { it.capitalize() }

            _uiEvent.emit(UiEvent.ShowLoading)

            try {

                val authResult = auth.createUserWithEmailAndPassword(email, password)
                    .await()

                val user = authResult.user ?: throw Exception("User creation failed")

                val profileUpdates = userProfileChangeRequest {
                    displayName = formattedName
                    // photoUri = ... // Profil resmi eklemek isterseniz burada ayarlayabilirsiniz
                }

                user.updateProfile(profileUpdates).await()

                user.sendEmailVerification().await()

                val userMap = hashMapOf(
                    "email" to email,
                    "name" to name,
                    "emailVerified" to false
                )

                Firebase.firestore.collection("Users").document(user.uid).set(userMap).await()

                //Hesap olusturuldu dogrulama ekranina gonder

                _uiEvent.emit(UiEvent.AccountCreated)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast(e.localizedMessage ?: "An error occurred"))
            }

            /*
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid


                    val profileUpdates = userProfileChangeRequest {
                        displayName = formattedName
                        // photoUri = ... // Profil resmi eklemek isterseniz burada ayarlayabilirsiniz
                    }

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (!profileTask.isSuccessful) {
                            _toastMessage.value = "Profile update failed"
                        }
                    }

                    // E-posta doğrulama bağlantısı gönder
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            _toastMessage.value = "Verification email sent to ${user.email}"
                        } else {
                            _toastMessage.value =
                                "Failed to send verification email: ${verificationTask.exception?.localizedMessage}"
                        }
                    }

                    val userMap = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "emailVerified" to false
                    )

                    if (uid != null) {
                        Firebase.firestore.collection("Users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                _isloading.value = false
                                _toastMessage.value =
                                    "Account created. Please verify your email before logging in."
                                _viewSuccessDialog.value = true
                                // E-posta doğrulama bilgilendirme sayfasına yönlendir
                                _navigateVerify.value = true
                            }.addOnFailureListener { e ->
                                _isloading.value = false
                                _toastMessage.value =
                                    "User saved but info failed: ${e.localizedMessage}"
                            }


                    }

                }


            }.addOnFailureListener { e ->
                _isloading.value = false
                _toastMessage.value = e.localizedMessage

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