package com.goodwy.calendar.activities

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CalendarContract
import androidx.core.app.NotificationManagerCompat
import com.goodwy.calendar.BuildConfig
import com.goodwy.calendar.R
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.getAlarmManager
import com.goodwy.calendar.extensions.refreshCalDAVCalendars
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.dialogs.PermissionRequiredDialog
import com.goodwy.commons.extensions.openNotificationSettings
import com.goodwy.commons.extensions.openRequestExactAlarmSettings
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.commons.helpers.isSPlus
import com.goodwy.commons.helpers.isTiramisuPlus

open class SimpleActivity : BaseSimpleActivity() {
    val CALDAV_REFRESH_DELAY = 3000L
    val calDAVRefreshHandler = Handler()
    var calDAVRefreshCallback: (() -> Unit)? = null

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher,
        R.mipmap.ic_launcher_one,
        R.mipmap.ic_launcher_two,
        R.mipmap.ic_launcher_three,
        R.mipmap.ic_launcher_four,
        R.mipmap.ic_launcher_five,
        R.mipmap.ic_launcher_six,
        R.mipmap.ic_launcher_seven,
        R.mipmap.ic_launcher_eight,
        R.mipmap.ic_launcher_nine,
        R.mipmap.ic_launcher_ten,
        R.mipmap.ic_launcher_eleven
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    override fun getRepositoryName() = "Calendar"

    fun Context.syncCalDAVCalendars(callback: () -> Unit) {
        calDAVRefreshCallback = callback
        ensureBackgroundThread {
            val uri = CalendarContract.Calendars.CONTENT_URI
            contentResolver.unregisterContentObserver(calDAVSyncObserver)
            contentResolver.registerContentObserver(uri, false, calDAVSyncObserver)
            refreshCalDAVCalendars(config.caldavSyncedCalendarIds, true)
        }
    }

    // caldav refresh content observer triggers multiple times in a row at updating, so call the callback only a few seconds after the (hopefully) last one
    private val calDAVSyncObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (!selfChange) {
                calDAVRefreshHandler.removeCallbacksAndMessages(null)
                calDAVRefreshHandler.postDelayed({
                    ensureBackgroundThread {
                        unregisterObserver()
                        calDAVRefreshCallback?.invoke()
                        calDAVRefreshCallback = null
                    }
                }, CALDAV_REFRESH_DELAY)
            }
        }
    }

    override fun onStop() {
        unregisterObserver()
        super.onStop()
    }

    private fun unregisterObserver() {
        contentResolver.unregisterContentObserver(calDAVSyncObserver)
    }

    protected fun handleNotificationAvailability(callback: () -> Unit) {
        handleNotificationPermission { granted ->
            if (granted) {
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    callback()
                } else {
                    ConfirmationDialog(
                        activity = this,
                        messageId = com.goodwy.commons.R.string.notifications_disabled,
                        positive = com.goodwy.commons.R.string.ok,
                        negative = 0
                    ) {
                        callback()
                    }
                }
            } else {
                PermissionRequiredDialog(
                    this,
                    com.goodwy.commons.R.string.allow_notifications_reminders,
                    { openNotificationSettings() })
            }
        }
    }

    fun maybeRequestExactAlarmPermission(callback: () -> Unit = {}) {
        if (isSPlus() && !isTiramisuPlus()) {
            // SCHEDULE_EXACT_ALARM *may* be revoked by users/system on Android 12
            if (getAlarmManager().canScheduleExactAlarms()) {
                callback()
            } else {
                PermissionRequiredDialog(
                    activity = this,
                    textId = R.string.allow_alarms_reminders,
                    positiveActionCallback = {
                        openRequestExactAlarmSettings(BuildConfig.APPLICATION_ID)
                    },
                )
            }
        } else {
            callback()
        }
    }
}
