package com.chnkcksk.reminderapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.PendingIntent
import android.content.Intent
import androidx.navigation.NavDeepLinkBuilder
import com.chnkcksk.reminderapp.R


class WorkerNotification(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        // inputData'dan bildirim parametrelerini alıyoruz
        // Eğer parametre gönderilmemişse varsayılan değerler kullanılır
        val title = inputData.getString("title") ?: "Varsayılan Başlık"
        val message = inputData.getString("message") ?: "Varsayılan Mesaj"
        val channelId = inputData.getString("channelId") ?: "default_channel"
        val channelName = inputData.getString("channelName") ?: "Genel Bildirimler"
        val channelDescription = inputData.getString("channelDescription") ?: "Uygulama bildirimleri"
        // Bildirim ikonu (varsayılan olarak Android'in info ikonu)
        val iconResId = inputData.getInt("iconResId", android.R.drawable.ic_dialog_info)
        // Bildirimi tıklayınca otomatik kapanma ayarı (varsayılan: true)
        val autoCancel = inputData.getBoolean("autoCancel", true)
        // Bildirim öncelik seviyesi (varsayılan: normal öncelik)
        val priority = inputData.getInt("priority", NotificationCompat.PRIORITY_DEFAULT)
        // Titreşim ayarı (varsayılan: açık)
        val vibrate = inputData.getBoolean("vibrate", true)
        // Ses ayarı (varsayılan: açık)
        val sound = inputData.getBoolean("sound", true)

        // try-catch bloğu ile hata yönetimi
        return try {
            // Önce bildirim kanalını oluştur (Android 8.0+ için gerekli)
            createNotificationChannel(channelId, channelName, channelDescription)

            // Bildirimi göster
            showNotification(title, message, channelId, iconResId, autoCancel, priority, vibrate, sound)

            // İşlem başarılı olduğunu bildir
            Result.success()
        } catch (e: Exception) {
            // Hata durumunda başarısızlık sonucu döndür
            Result.failure()
        }
    }

    /**
     * Bildirim kanalı oluşturma fonksiyonu
     * Android 8.0 (API 26) ve üstü sürümler için bildirim kanalı oluşturur
     */
    private fun createNotificationChannel(channelId: String, channelName: String, channelDescription: String) {
        // Android 8.0 ve üstü sürümlerde bildirim kanalı zorunlu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Kanal önem seviyesi (normal seviye)
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            // Bildirim kanalını oluştur
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            // NotificationManager servisini al ve kanalı kaydet
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Bildirim gösterme fonksiyonu
     * Tüm parametreleri kullanarak bildirim oluşturur ve gösterir
     */
    private fun showNotification(
        title: String,           
        message: String,         
        channelId: String,       
        iconResId: Int,          
        autoCancel: Boolean,     
        priority: Int,           
        vibrate: Boolean,        
        sound: Boolean           
    ) {
        // Create a PendingIntent for navigation
        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.core_nav_graph)
            .setDestination(R.id.homeFragment)
            .createPendingIntent()

        // NotificationCompat.Builder ile bildirim oluşturucu
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconResId)        
            .setContentTitle(title)         
            .setContentText(message)        
            .setPriority(priority)          
            .setAutoCancel(autoCancel)      
            .setContentIntent(pendingIntent) // Add the PendingIntent here

        // Titreşim ayarını kontrol et ve uygula
        if (vibrate) {
            // Titreşim paterni: 1 saniye titreşim, 1 saniye duraklama (5 kez tekrar)
            notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        }

        // Ses ayarını kontrol et ve uygula
        if (sound) {
            // Varsayılan bildirim sesini kullan
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        // NotificationManager servisini al
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bildirimi göster (benzersiz ID olarak sistem zamanını kullan)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}