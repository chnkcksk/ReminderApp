package com.chnkcksk.reminderapp.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthManager {


    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Kullanıcının giriş yapıp yapmadığını kontrol et
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Giriş yapmış kullanıcının bilgilerini döndür
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // Kullanıcı çıkışı yap
    fun signOut() {
        auth.signOut()
    }

}