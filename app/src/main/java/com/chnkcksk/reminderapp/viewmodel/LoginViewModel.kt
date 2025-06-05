package com.chnkcksk.reminderapp.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Email
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chnkcksk.reminderapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean> get() = _navigateToHome

    private val _navigateVerify = MutableLiveData<Boolean>()
    val navigateVerify: LiveData<Boolean> get() = _navigateVerify

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    // Google Sign-In Client
    private var googleSignInClient: GoogleSignInClient? = null

    fun initializeGoogleSignIn(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun getGoogleSignInIntent(): Intent? {
        return googleSignInClient?.signInIntent
    }

    fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        _isLoading.value = true
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            _isLoading.value = false
            _toastMessage.value = "Google sign in failed: ${e.localizedMessage}"
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        // Kullanıcının Firestore'da mevcut olup olmadığını kontrol et
                        firestore.collection("Users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Yeni kullanıcı, Firestore'a kaydet
                                    val userMap = hashMapOf(
                                        "email" to user.email,
                                        "name" to user.displayName,
                                        "emailVerified" to true // Google hesapları zaten doğrulanmış
                                    )

                                    firestore.collection("Users").document(uid).set(userMap)
                                        .addOnSuccessListener {
                                            _isLoading.value = false
                                            _toastMessage.value = "Welcome! Account created successfully."
                                            _navigateToHome.value = true
                                        }
                                        .addOnFailureListener { e ->
                                            _isLoading.value = false
                                            _toastMessage.value = "Failed to save user info: ${e.localizedMessage}"
                                        }



                                } else {
                                    // Mevcut kullanıcı
                                    _isLoading.value = false
                                    _toastMessage.value = "Welcome back!"
                                    _navigateToHome.value = true
                                }
                            }
                            .addOnFailureListener { e ->
                                _isLoading.value = false
                                _toastMessage.value = "Database error: ${e.localizedMessage}"
                            }
                    }
                } else {
                    _isLoading.value = false
                    _toastMessage.value = "Authentication failed: ${task.exception?.localizedMessage}"
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

    fun login(email: String, password: String) {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            _isLoading.value = true

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        user?.reload()?.addOnCompleteListener { reloadTask ->
                            if (reloadTask.isSuccessful) {
                                if (user.isEmailVerified) {
                                    _navigateToHome.value = true
                                } else {
                                    auth.signOut()
                                    _toastMessage.value = "Please verify your email before logging in."
                                    _navigateVerify.value = true
                                }
                            } else {
                                _toastMessage.value = "Failed to verify email status. Please try again."
                            }
                            _isLoading.value = false
                        }

                    }
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    _toastMessage.value = e.localizedMessage
                }

        } else {
            _isLoading.value = false
            _toastMessage.value = "Please enter email and password!"
        }
    }

}