package com.goodwy.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.goodwy.calendar.extensions.eventsDB
import com.goodwy.calendar.extensions.notifyEvent
import com.goodwy.calendar.extensions.scheduleNextEventReminder
import com.goodwy.calendar.extensions.updateListWidget
import com.goodwy.calendar.helpers.EVENT_ID
import com.goodwy.calendar.helpers.Formatter
import com.goodwy.calendar.helpers.REMINDER_NOTIFICATION
import com.goodwy.commons.helpers.ensureBackgroundThread

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "simplecalendar:notificationreceiver")
        wakelock.acquire(3000)

        ensureBackgroundThread {
            handleIntent(context, intent)
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EVENT_ID, -1L)
        if (id != -1L) {
            context.updateListWidget()
            val event = context.eventsDB.getEventOrTaskWithId(id)
            if (
                event == null
                || event.getReminders().none { it.type == REMINDER_NOTIFICATION }
                || event.repetitionExceptions.contains(Formatter.getTodayCode())
            ) {
                return
            }

            if (event.isAttendeeInviteDeclined()) {
                return
            }

            if (!event.repetitionExceptions.contains(Formatter.getDayCodeFromTS(event.startTS))) {
                context.notifyEvent(event)
            }
            context.scheduleNextEventReminder(event, false)
        }
    }
}
