package com.chnkcksk.reminderapp.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.provider.ContactsContract.CommonDataKinds.Email
import android.text.method.PasswordTransformationMethod
import android.util.Log
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class GoogleUser(val message: String) : UiEvent()
        object VerifyEmail:UiEvent()

        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        object NavigateHome : UiEvent()
        object NavigateVerify : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

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
        viewModelScope.launch {
            Log.d("LoginViewModel", "handleGoogleSignInResult called")
            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginViewModel", "Google account obtained: ${account.email}")

                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {
                Log.e("LoginViewModel", "Google Sign-In failed", e)
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Google sign in failed: ${e.localizedMessage}"))
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("LoginViewModel", "firebaseAuthWithGoogle called")
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        try {
            // Firebase Auth ile giriş yap
            Log.d("LoginViewModel", "Attempting Firebase authentication")
            val authResult = auth.signInWithCredential(credential).await()
            Log.d("LoginViewModel", "Firebase authentication successful")

            if (authResult.user == null) {
                Log.e("LoginViewModel", "Auth result user is null")
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Authentication failed"))
                return
            }

            val user = auth.currentUser
            val uid = user?.uid
            Log.d("LoginViewModel", "User UID: $uid, Email: ${user?.email}")

            if (uid == null) {
                Log.e("LoginViewModel", "User UID is null")
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Failed to get user ID"))
                return
            }

            // Kullanıcının Firestore'da mevcut olup olmadığını kontrol et
            Log.d("LoginViewModel", "Checking Firestore for user document")
            val document = firestore.collection("Users")
                .document(uid)
                .get()
                .await()

            if (!document.exists()) {
                // Yeni kullanıcı, Firestore'a kaydet
                Log.d("LoginViewModel", "Creating new user in Firestore")
                val userMap = hashMapOf(
                    "email" to user.email,
                    "name" to user.displayName,
                    "emailVerified" to true // Google hesapları zaten doğrulanmış
                )

                firestore.collection("Users")
                    .document(uid)
                    .set(userMap)
                    .await()

                Log.d("LoginViewModel", "New user created successfully")
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.GoogleUser("Welcome! Account created successfully."))

            } else {
                Log.d("LoginViewModel", "Existing user found")
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.GoogleUser("Welcome back!"))
                // Mevcut kullanıcı
            }

        } catch (e: Exception) {
            Log.e("LoginViewModel", "Firebase authentication failed", e)
            _uiEvent.emit(UiEvent.HideLoading)
            _uiEvent.emit(UiEvent.ShowToast("Authentication failed: ${e.localizedMessage}"))
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
                _uiEvent.emit(UiEvent.ShowToast("Please enter email and password!"))
                return@launch
            }

            try {
                _uiEvent.emit(UiEvent.ShowLoading)

                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("User not found")

                user.reload().await()

                if (user.isEmailVerified) {
                    _uiEvent.emit(UiEvent.HideLoading)
                    _uiEvent.emit(UiEvent.NavigateHome)
                } else {
                    auth.signOut()

                    _uiEvent.emit(UiEvent.VerifyEmail)
                }

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast(e.localizedMessage ?: "Login failed"))
            }
            //finally { }
        }
    }

}