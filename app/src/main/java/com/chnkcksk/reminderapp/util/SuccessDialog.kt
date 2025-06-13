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

class SuccessDialog  {


     fun showSuccessDialog(context: Context){
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_dialog)

        // Dialog penceresi ayarları
        val window = dialog.window
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Animasyon bittikten sonra otomatik kapanması için
        val lottieView = dialog.findViewById<LottieAnimationView>(R.id.success_animation)
        lottieView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                // Animasyon bittikten sonra 500ms bekleyip kapat
                Handler(Looper.getMainLooper()).postDelayed({
                    dialog.dismiss()
                    //goBack() // İşlem tamamlandığında önceki ekrana dön
                }, 300) // 300ms bekleme süresi - isteğe göre ayarlayabilirsiniz
            }
        })



        dialog.show()
    }

}