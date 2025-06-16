package com.chnkcksk.reminderapp.util

import android.animation.Animator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import com.airbnb.lottie.LottieAnimationView
import com.chnkcksk.reminderapp.R

class SuccessDialog {

    fun showSuccessDialog(context: Context, onComplete: (() -> Unit)? = null) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_dialog)

        val window = dialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val lottieView = dialog.findViewById<LottieAnimationView>(R.id.success_animation)
        lottieView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                    onComplete?.invoke() // Animasyon + dialog kapandıktan sonra işlem
                }, 300)
            }
        })

        dialog.show()
    }
}
