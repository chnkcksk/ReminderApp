package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateVerify = MutableLiveData<Boolean>()
    val navigateVerify: LiveData<Boolean> get() = _navigateVerify

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _isloading = MutableLiveData<Boolean>()
    val isloading: LiveData<Boolean> get() = _isloading

    private val auth: FirebaseAuth = Firebase.auth

    fun register(name: String, email: String, password: String) {

        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {

            _isloading.value = true

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
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

        } else {
            _toastMessage.value = "Please fill empty fields"
        }

    }

}