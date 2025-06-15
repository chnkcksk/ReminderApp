package com.chnkcksk.reminderapp.widgets

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReminderWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val itemList = mutableListOf<Reminder>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        itemList.clear()

        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        val latch = CountDownLatch(1)

        firestore.collection("Users")
            .document(user.uid)
            .collection("workspaces")
            .document("personalWorkspace")
            .collection("reminders")
            .get()
            .addOnSuccessListener { documents ->
                val tempList = ArrayList<Reminder>()

                for (document in documents) {
                    val reminder = Reminder(
                        id = document.id,
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        isCompleted = document.getBoolean("isCompleted") ?: false,
                        timestamp = document.get("timestamp").toString() ?: "",
                        priority = document.getString("priority") ?: "",
                        date = document.getString("date") ?: "",
                        time = document.getString("time") ?: "",
                        reminder = document.getBoolean("reminder") ?: false
                    )
                    tempList.add(reminder)
                }

                tempList.sortByDescending { reminder ->
                    try {
                        reminder.timestamp.toLong()
                    } catch (e: Exception) {
                        0L
                    }
                }

                itemList.addAll(tempList.take(3))
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        latch.await(5, TimeUnit.SECONDS)
    }

    override fun getCount(): Int = itemList.size

    override fun getViewAt(position: Int): RemoteViews {
        val reminder = itemList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_reminder_item)

        views.setTextViewText(R.id.reminderTitle, reminder.title)
        views.setTextViewText(R.id.reminderDateTime, "${reminder.date}, ${reminder.time}")

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {}
}