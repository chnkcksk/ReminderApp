package com.chnkcksk.reminderapp.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.chnkcksk.reminderapp.R
import java.util.concurrent.TimeUnit

class LoadingManager private constructor() {
    private var loadingDialog: Dialog? = null
    private var timeoutHandler = Handler(Looper.getMainLooper())
    private var displayHandler = Handler(Looper.getMainLooper())
    private var fadeInAnimation: Animation? = null
    private var fadeOutAnimation: Animation? = null
    private var isLoadingRequested = false
    private var requestStartTime = 0L

    companion object {
        private const val DEFAULT_TIMEOUT = 10000L // 10 saniye
        private const val MIN_SHOW_TIME = 1000L // Minimum gösterim süresi (ms)
        private const val DISPLAY_DELAY = 200L // 200ms sonra göster
        private var instance: LoadingManager? = null

        fun getInstance(): LoadingManager {
            if (instance == null) {
                instance = LoadingManager()
            }
            return instance!!
        }
    }

    // Yükleme dialogunu göster
    fun showLoading(
        context: Context,
        message: String = "Loading..",
        timeout: Long = DEFAULT_TIMEOUT
    ) {
        // İstek zamanını kaydet
        isLoadingRequested = true
        requestStartTime = System.currentTimeMillis()

        // Mevcut dialogu temizle
        if (loadingDialog?.isShowing == true) {
            dismissLoading()
        }

        // 200ms gecikmeli gösterim için handler kullan
        displayHandler.removeCallbacksAndMessages(null)
        displayHandler.postDelayed({
            // Eğer hala yükleme gösterilmesi gerekiyorsa
            if (isLoadingRequested) {
                createAndShowDialog(context, message)

                // Zaman aşımı kontrolü
                timeoutHandler.postDelayed({
                    dismissLoading()
                    // Zaman aşımında toast mesajı göster
                    if (context is Activity && !context.isFinishing) {
                        Toast.makeText(context, "This operation has timed out", Toast.LENGTH_SHORT)
                            .show()
                    }
                }, timeout)
            }
        }, DISPLAY_DELAY)
    }

    private fun createAndShowDialog(context: Context, message: String) {
        // Yeni dialog oluştur
        loadingDialog = Dialog(context)
        loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog?.setContentView(R.layout.loading_dialog)
        loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog?.setCancelable(false)

        // Lottie animasyonunu düzelt
        val lottieAnimationView =
            loadingDialog?.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.loading_anim)
        lottieAnimationView?.let {
            // Loop'u true yap
            it.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
            it.repeatMode = com.airbnb.lottie.LottieDrawable.RESTART
            // Animasyonu yeniden başlat
            it.playAnimation()
        }

        // Mesajı ayarla (eğer varsa)
//        val messageTextView = loadingDialog?.findViewById<TextView>(R.id.loadingMessage)
//        messageTextView?.text = message

        // Animasyonları yükle
        fadeInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        fadeOutAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)

        // Dialogu göster ve fade-in animasyonunu başlat
        loadingDialog?.show()
        loadingDialog?.window?.decorView?.startAnimation(fadeInAnimation)
    }

    // Yükleme dialogunu kapat
    fun dismissLoading(callback: (() -> Unit)? = null) {
        isLoadingRequested = false
        displayHandler.removeCallbacksAndMessages(null)
        timeoutHandler.removeCallbacksAndMessages(null)

        if (loadingDialog?.isShowing == true) {
            val displayTime = System.currentTimeMillis() - requestStartTime
            val remainingMinTime = Math.max(0, MIN_SHOW_TIME - displayTime)

            try {
                if (loadingDialog?.window?.decorView?.windowToken != null) {
                    loadingDialog?.window?.decorView?.startAnimation(fadeOutAnimation)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            timeoutHandler.postDelayed({
                try {
                    if (loadingDialog?.isShowing == true) {
                        loadingDialog?.dismiss()
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } finally {
                    loadingDialog = null
                    callback?.invoke() // Callback'i çağır
                }
            }, remainingMinTime)
        } else {
            callback?.invoke() // Dialog zaten kapalıysa callback'i hemen çağır
        }
    }


    // Activity kapatıldığında çağrılmalı
    fun onDestroy() {
        dismissLoading()
        displayHandler.removeCallbacksAndMessages(null)
        timeoutHandler.removeCallbacksAndMessages(null)
    }
}