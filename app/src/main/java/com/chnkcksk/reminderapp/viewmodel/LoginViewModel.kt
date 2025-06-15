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
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            viewModelScope.launch {
                firebaseAuthWithGoogle(account.idToken!!)
            }

        } catch (e: ApiException) {
            _isLoading.value = false
            _toastMessage.value = "Google sign in failed: ${e.localizedMessage}"
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        try {
            // Firebase Auth ile giriş yap
            val authResult = auth.signInWithCredential(credential).await()

            if (authResult.user != null) {
                val user = auth.currentUser
                val uid = user?.uid

                if (uid != null) {
                    // Kullanıcının Firestore'da mevcut olup olmadığını kontrol et
                    val document = firestore.collection("Users")
                        .document(uid)
                        .get()
                        .await()

                    if (!document.exists()) {
                        // Yeni kullanıcı, Firestore'a kaydet
                        val userMap = hashMapOf(
                            "email" to user.email,
                            "name" to user.displayName,
                            "emailVerified" to true // Google hesapları zaten doğrulanmış
                        )

                        firestore.collection("Users")
                            .document(uid)
                            .set(userMap)
                            .await()

                        _isLoading.value = false
                        delay(1200)
                        _toastMessage.value = "Welcome! Account created successfully."
                        delay(500)
                        _navigateToHome.value = true

                    } else {
                        // Mevcut kullanıcı
                        _isLoading.value = false
                        delay(1200)
                        _toastMessage.value = "Welcome back!"
                        delay(500)
                        _navigateToHome.value = true
                    }
                } else {
                    _isLoading.value = false
                    delay(1200)
                    _toastMessage.value = "Failed to get user ID"
                }
            } else {
                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "Authentication failed"
            }

        } catch (e: Exception) {
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = "Authentication failed: ${e.localizedMessage}"
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

        viewModelScope.launch {

        if (email.isEmpty() || password.isEmpty()) {
            _isLoading.value = false
            _toastMessage.value = "Please enter email and password!"
            return@launch
        }

        try {
            _isLoading.value = true

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User not found")

            user.reload().await()

            if (user.isEmailVerified) {
                _isLoading.value = false
                delay(1200)
                _navigateToHome.value = true
            } else {
                auth.signOut()
                _isLoading.value = false
                delay(1200)
                _toastMessage.value = "Please verify your email before logging in."
                delay(500)
                _navigateVerify.value = true
            }

        } catch (e: Exception) {
            _isLoading.value = false
            delay(1200)
            _toastMessage.value = e.localizedMessage ?: "Login failed"
        }
        //finally { }
        }
    }

}