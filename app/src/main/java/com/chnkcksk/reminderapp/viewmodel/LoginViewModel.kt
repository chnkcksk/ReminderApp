package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.lifecycle.AndroidViewModel
import com.chnkcksk.reminderapp.R

class LoginViewModel(application: Application) : AndroidViewModel(application) {

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