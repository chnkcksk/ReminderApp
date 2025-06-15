package com.chnkcksk.reminderapp.widgets

import android.content.Intent
import android.widget.RemoteViewsService

class ReminderWidgetService:RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ReminderWidgetFactory(applicationContext)
    }
}