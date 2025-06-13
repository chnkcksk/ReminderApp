package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chnkcksk.reminderapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateVerify = MutableLiveData<Boolean>()
    val navigateVerify: LiveData<Boolean> get() = _navigateVerify

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _isloading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isloading

    private val _viewSuccessDialog = MutableLiveData<Boolean>()
    val viewSuccessDialog: LiveData<Boolean> get() = _viewSuccessDialog

    private val auth: FirebaseAuth = Firebase.auth

    suspend fun register(name: String, email: String, password: String) {

        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            _toastMessage.value = "Please fill empty fields"
            return
        }

        val formattedName = name.lowercase().split(" ").joinToString(" ") { it.capitalize() }

        _isloading.value = true

        try {

            val authResult = auth.createUserWithEmailAndPassword(email,password)
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

            _isloading.value = false
            delay(1200)
            _viewSuccessDialog.value = true
            delay(2000)
            _navigateVerify.value = true

        }catch (e:Exception){
            _isloading.value = false
            delay(1200)
            _toastMessage.value = e.localizedMessage ?: "An error occurred"
        }


//        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val user = auth.currentUser
//                val uid = user?.uid
//
//
//                val profileUpdates = userProfileChangeRequest {
//                    displayName = formattedName
//                    // photoUri = ... // Profil resmi eklemek isterseniz burada ayarlayabilirsiniz
//                }
//
//                user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
//                    if (!profileTask.isSuccessful) {
//                        _toastMessage.value = "Profile update failed"
//                    }
//                }
//
//                // E-posta doğrulama bağlantısı gönder
//                user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
//                    if (verificationTask.isSuccessful) {
//                        _toastMessage.value = "Verification email sent to ${user.email}"
//                    } else {
//                        _toastMessage.value =
//                            "Failed to send verification email: ${verificationTask.exception?.localizedMessage}"
//                    }
//                }
//
//                val userMap = hashMapOf(
//                    "email" to email,
//                    "name" to name,
//                    "emailVerified" to false
//                )
//
//                if (uid != null) {
//                    Firebase.firestore.collection("Users").document(uid).set(userMap)
//                        .addOnSuccessListener {
//                            _isloading.value = false
//                            _toastMessage.value =
//                                "Account created. Please verify your email before logging in."
//                            _viewSuccessDialog.value = true
//                            // E-posta doğrulama bilgilendirme sayfasına yönlendir
//                            _navigateVerify.value = true
//                        }.addOnFailureListener { e ->
//                            _isloading.value = false
//                            _toastMessage.value =
//                                "User saved but info failed: ${e.localizedMessage}"
//                        }
//
//
//                }
//
//            }
//
//
//        }.addOnFailureListener { e ->
//            _isloading.value = false
//            _toastMessage.value = e.localizedMessage
//
//        }


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