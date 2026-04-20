package com.goodwy.calendar.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goodwy.calendar.extensions.notifyRunningEvents
import com.goodwy.calendar.jobs.AppStartupWorker
import com.goodwy.commons.helpers.ensureBackgroundThread

class RescheduleEventsReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        AppStartupWorker.start(
            context = context,
            replaceExistingWork = action == Intent.ACTION_TIME_CHANGED
                    || action == Intent.ACTION_TIMEZONE_CHANGED
        )

        val shouldNotifyRunningEvents = action != Intent.ACTION_TIME_CHANGED
                && action != Intent.ACTION_TIMEZONE_CHANGED
                && action != Intent.ACTION_MY_PACKAGE_REPLACED

        if (shouldNotifyRunningEvents) {
            val result = goAsync()
            ensureBackgroundThread {
                context.notifyRunningEvents()
                result.finish()
            }
        }
    }
}

