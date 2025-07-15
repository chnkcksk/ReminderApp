package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.graphics.Color
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

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
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                _uiEvent.emit(UiEvent.ShowToast("Please fill all fields"))
                return@launch
            }

            val formattedName = name.lowercase().split(" ").joinToString(" ") { it.capitalize() }

            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                // 1. Kullanıcı oluştur
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw Exception("User creation failed")

                // 2. Profil güncelle
                val profileUpdates = userProfileChangeRequest {
                    displayName = formattedName
                }
                user.updateProfile(profileUpdates).await()

                // 3. Email verification gönder - Ayrı try-catch ile
                try {
                    user.sendEmailVerification().await()
                    _uiEvent.emit(UiEvent.ShowToast("Verification email sent to $email"))
                } catch (emailError: Exception) {
                    // Email verification hatası, ama hesap oluşturuldu
                    _uiEvent.emit(UiEvent.ShowToast("Account created but email verification failed: ${emailError.localizedMessage}"))
                }

                // 4. Firestore'a kaydet - Ayrı try-catch ile
                try {
                    val userMap = hashMapOf(
                        "email" to email,
                        "name" to formattedName,
                        "emailVerified" to false
                    )
                    Firebase.firestore.collection("Users").document(user.uid).set(userMap).await()
                    _uiEvent.emit(UiEvent.ShowToast("Account created successfully!"))
                } catch (firestoreError: Exception) {
                    // Firestore hatası, ama hesap ve email verification başarılı
                    _uiEvent.emit(UiEvent.ShowToast("Account created but profile data save failed: ${firestoreError.localizedMessage}"))
                }

                _uiEvent.emit(UiEvent.AccountCreated)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Account creation failed: ${e.localizedMessage}"))
            }
        }
    }



    fun togglePasswordVisibility(editText: EditText, toggleButton: ImageButton) {
        if (editText.transformationMethod is PasswordTransformationMethod) {
            editText.transformationMethod = null
            toggleButton.setImageResource(R.drawable.baseline_visibility_24)
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            toggleButton.setImageResource(R.drawable.baseline_visibility_off_24)
        }
        editText.setSelection(editText.text.length)
    }
}