package com.chnkcksk.reminderapp.permissions

import android.Manifest
import android.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * TÜM ANDROID SÜRÜMLERİ İÇİN KAPSAMLI BİLDİRİM İZNİ YÖNETİCİSİ
 * Her sayfada özelleştirilebilir bildirim içeriği ile
 */
class NotificationPermissionManager {

    /**
     * Özelleştirilebilir bildirim içeriği ayarları
     */
    data class NotificationContent(
        val title: String = "Varsayılan Bildirim",
        val message: String = "Bu bir test bildirimidir",
        val channelId: String = "default_channel",
        val channelName: String = "Genel Bildirimler",
        val channelDescription: String = "Uygulama bildirimleri",
        val iconResId: Int = R.drawable.ic_dialog_info,
        val autoCancel: Boolean = true,
        val priority: Int = 2, // NotificationCompat.PRIORITY_DEFAULT
        val vibrate: Boolean = true,
        val sound: Boolean = true,
        val delaySeconds: Int = 10 // Kaç saniye sonra bildirim gönderilsin
    )

    interface NotificationPermissionCallback {
        fun onPermissionGranted(notificationContent: NotificationContent)
        fun onPermissionDenied()
        fun onNotificationsDisabled() // Yeni callback - bildirimler kapalı durumu için
        fun onSettingsOpened() {} // Opsiyonel
    }

    private var permissionCallback: NotificationPermissionCallback? = null
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null
    private var currentNotificationContent: NotificationContent = NotificationContent()

    /**
     * Activity için permission launcher'ını kaydet
     */
    fun registerPermissionLauncher(activity: AppCompatActivity): NotificationPermissionManager {
        notificationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                permissionCallback?.onPermissionGranted(currentNotificationContent)
            } else {
                permissionCallback?.onPermissionDenied()
            }
        }
        return this
    }

    /**
     * Fragment için permission launcher'ını kaydet
     */
    fun registerPermissionLauncher(fragment: Fragment): NotificationPermissionManager {
        notificationPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                permissionCallback?.onPermissionGranted(currentNotificationContent)
            } else {
                permissionCallback?.onPermissionDenied()
            }
        }
        return this
    }

    /**
     * Ana bildirim izni kontrol fonksiyonu - özelleştirilebilir bildirim içeriği ile
     */
    fun checkNotificationPermission(
        context: Context,
        callback: NotificationPermissionCallback,
        notificationContent: NotificationContent
    ) {
        this.permissionCallback = callback
        this.currentNotificationContent = notificationContent

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                handleAndroid13Plus(context)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                handleAndroid8Plus(context)
            }
            else -> {
                handleAndroidLegacy(context)
            }
        }
    }

    /**
     * Varsayılan bildirim içeriği ile basit kullanım
     */
    fun checkNotificationPermission(
        context: Context,
        callback: NotificationPermissionCallback
    ) {
        checkNotificationPermission(context, callback, NotificationContent())
    }

    /**
     * Sadece izin durumunu kontrol et (dialog göstermeden)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED && areNotificationsEnabled(context)
            }
            else -> {
                areNotificationsEnabled(context)
            }
        }
    }

    private fun handleAndroid13Plus(context: Context) {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            PackageManager.PERMISSION_GRANTED -> {
                if (areNotificationsEnabled(context)) {
                    permissionCallback?.onPermissionGranted(currentNotificationContent)
                } else {
                    permissionCallback?.onNotificationsDisabled()
                }
            }
            else -> {
                notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                    ?: permissionCallback?.onPermissionDenied()
            }
        }
    }

    private fun handleAndroid8Plus(context: Context) {
        if (areNotificationsEnabled(context)) {
            permissionCallback?.onPermissionGranted(currentNotificationContent)
        } else {
            permissionCallback?.onNotificationsDisabled()
        }
    }

    private fun handleAndroidLegacy(context: Context) {
        if (areNotificationsEnabled(context)) {
            permissionCallback?.onPermissionGranted(currentNotificationContent)
        } else {
            permissionCallback?.onNotificationsDisabled()
        }
    }

    private fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Ayarları açmak için yardımcı fonksiyon
     */
    fun openNotificationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                Toast.makeText(context, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Uygulama detay ayarlarını açmak için yardımcı fonksiyon
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                Toast.makeText(context, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NotificationPermissionManager? = null

        fun getInstance(): NotificationPermissionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationPermissionManager().also { INSTANCE = it }
            }
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED &&
                            NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
                else -> {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            }
        }
    }
}