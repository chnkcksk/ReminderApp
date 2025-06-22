package com.chnkcksk.reminderapp.widgets

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast
import com.chnkcksk.reminderapp.R
import java.text.SimpleDateFormat
import java.util.*

class ReminderWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.chnkcksk.reminderapp.ACTION_REFRESH"
        const val ACTION_AUTO_UPDATE = "com.chnkcksk.reminderapp.ACTION_AUTO_UPDATE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Widget ilk eklendiğinde veya güncellendiğinde verileri yükle
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            // Verileri hemen yenile
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
        }

        // Saatlik otomatik güncelleme için alarm ayarla
        if (hasAlarmPermission(context)) {
            setAlarmForHourlyUpdate(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                // Manuel refresh
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ReminderWidget::class.java)
                )

                for (appWidgetId in appWidgetIds) {
                    // Widget'i güncelle
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
                    updateLastUpdateTime(context, appWidgetManager, appWidgetId)
                }
            }

            ACTION_AUTO_UPDATE -> {
                // Otomatik saatlik güncelleme
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ReminderWidget::class.java)
                )

                for (appWidgetId in appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
                    updateLastUpdateTime(context, appWidgetManager, appWidgetId)
                }

                // Bir sonraki saatlik güncelleme için alarm ayarla
                if (hasAlarmPermission(context)) {
                    setAlarmForHourlyUpdate(context)
                }
            }
        }
    }

    private fun hasAlarmPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val intent = Intent(context, ReminderWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val views = RemoteViews(context.packageName, R.layout.reminder_widget)
        views.setRemoteAdapter(R.id.widgetListView, intent)
        views.setEmptyView(R.id.widgetListView, android.R.id.empty)

        // Refresh butonu için PendingIntent
        val refreshIntent = Intent(context, ReminderWidget::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            refreshIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent)

        // İlk güncelleme zamanını ayarla
        updateLastUpdateTime(context, appWidgetManager, appWidgetId)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateLastUpdateTime(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.reminder_widget)
        val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.lastUpdateTime, "Last Update: $currentTime")
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarmForHourlyUpdate(context: Context) {
        if (!hasAlarmPermission(context)) {
            Toast.makeText(
                context,
                "Widget güncellemesi için alarm izni gerekiyor. Lütfen ayarlardan izin verin.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderWidget::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            1, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Önce mevcut alarmı iptal et
        alarmManager.cancel(pendingIntent)

        // Bir sonraki saatin başında tetiklenecek şekilde ayarla
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Android 6.0+ için setExactAndAllowWhileIdle kullan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget ilk eklendiğinde alarm'ı ayarla
        if (hasAlarmPermission(context)) {
            setAlarmForHourlyUpdate(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Son widget kaldırıldığında alarm'ı iptal et
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderWidget::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            1, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}