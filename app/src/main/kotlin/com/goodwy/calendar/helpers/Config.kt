package com.goodwy.calendar.helpers

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import androidx.core.content.ContextCompat
import com.goodwy.calendar.R
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.scheduleCalDAVSync
import com.goodwy.commons.extensions.getDefaultAlarmTitle
import com.goodwy.commons.helpers.BaseConfig
import com.goodwy.commons.helpers.DAY_MINUTES
import com.goodwy.commons.helpers.YEAR_SECONDS
import androidx.core.content.edit

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(showWeekNumbers) = prefs.edit { putBoolean(WEEK_NUMBERS, showWeekNumbers) }

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit { putInt(START_WEEKLY_AT, startWeeklyAt) }

    var startWeekWithCurrentDay: Boolean
        get() = prefs.getBoolean(START_WEEK_WITH_CURRENT_DAY, false)
        set(startWeekWithCurrentDay) = prefs.edit { putBoolean(START_WEEK_WITH_CURRENT_DAY, startWeekWithCurrentDay) }

    var showMidnightSpanningEventsAtTop: Boolean
        get() = prefs.getBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, true)
        set(midnightSpanning) = prefs.edit { putBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, midnightSpanning) }

    var allowCustomizeDayCount: Boolean
        get() = prefs.getBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, true)
        set(allow) = prefs.edit { putBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, allow) }

    var vibrateOnReminder: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(vibrate) = prefs.edit { putBoolean(VIBRATE, vibrate) }

    @Deprecated("Not used on Oreo+ devices")
    var reminderSoundUri: String
        get() = prefs.getString(
            REMINDER_SOUND_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        )!!
        set(reminderSoundUri) = prefs.edit { putString(REMINDER_SOUND_URI, reminderSoundUri) }

    @Deprecated("Not used on Oreo+ devices")
    var reminderSoundTitle: String
        get() = prefs.getString(
            REMINDER_SOUND_TITLE,
            context.getDefaultAlarmTitle(RingtoneManager.TYPE_NOTIFICATION)
        )!!
        set(reminderSoundTitle) = prefs.edit { putString(REMINDER_SOUND_TITLE, reminderSoundTitle) }

    @Deprecated("Not used on Oreo+ devices")
    var lastSoundUri: String
        get() = prefs.getString(LAST_SOUND_URI, "")!!
        set(lastSoundUri) = prefs.edit { putString(LAST_SOUND_URI, lastSoundUri) }

    var lastReminderChannel: Long
        get() = prefs.getLong(LAST_REMINDER_CHANNEL_ID, 0L)
        set(lastReminderChannel) = prefs.edit { putLong(LAST_REMINDER_CHANNEL_ID, lastReminderChannel) }

    var storedView: Int
        get() = prefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = prefs.edit { putInt(VIEW, view) }

    var lastEventReminderMinutes1: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES, 10)
        set(lastEventReminderMinutes) = prefs.edit { putInt(LAST_EVENT_REMINDER_MINUTES, lastEventReminderMinutes) }

    var lastEventReminderMinutes2: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_2, REMINDER_OFF)
        set(lastEventReminderMinutes2) = prefs.edit { putInt(LAST_EVENT_REMINDER_MINUTES_2, lastEventReminderMinutes2) }

    var lastEventReminderMinutes3: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_3, REMINDER_OFF)
        set(lastEventReminderMinutes3) = prefs.edit { putInt(LAST_EVENT_REMINDER_MINUTES_3, lastEventReminderMinutes3) }

    var displayPastEvents: Int
        get() = prefs.getInt(DISPLAY_PAST_EVENTS, DAY_MINUTES)
        set(displayPastEvents) = prefs.edit { putInt(DISPLAY_PAST_EVENTS, displayPastEvents) }

    var displayCalendars: Set<String>
        get() = prefs.getStringSet(DISPLAY_CALENDARS, HashSet())!!
        set(displayCalendars) = prefs.edit {
            remove(DISPLAY_CALENDARS)
                .putStringSet(DISPLAY_CALENDARS, displayCalendars)
        }

    var quickFilterCalendars: Set<String>
        get() = prefs.getStringSet(QUICK_FILTER_CALENDARS, HashSet())!!
        set(quickFilterCalendars) = prefs.edit {
            remove(QUICK_FILTER_CALENDARS)
                .putStringSet(QUICK_FILTER_CALENDARS, quickFilterCalendars)
        }

    fun addQuickFilterCalendar(type: String) {
        val currQuickFilterCalendars = HashSet(quickFilterCalendars)
        currQuickFilterCalendars.add(type)
        quickFilterCalendars = currQuickFilterCalendars
    }

    var listWidgetViewToOpen: Int
        get() = prefs.getInt(LIST_WIDGET_VIEW_TO_OPEN, DAILY_VIEW)
        set(viewToOpenFromListWidget) = prefs.edit { putInt(LIST_WIDGET_VIEW_TO_OPEN, viewToOpenFromListWidget) }

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleCalDAVSync(caldavSync)
            prefs.edit { putBoolean(CALDAV_SYNC, caldavSync) }
        }

    var caldavSyncedCalendarIds: String
        get() = prefs.getString(CALDAV_SYNCED_CALENDAR_IDS, "")!!
        set(calendarIDs) = prefs.edit { putString(CALDAV_SYNCED_CALENDAR_IDS, calendarIDs) }

    var lastUsedCaldavCalendarId: Int
        get() = prefs.getInt(
            LAST_USED_CALDAV_CALENDAR,
            getSyncedCalendarIdsAsList().firstOrNull() ?: STORED_LOCALLY_ONLY
        )
        set(calendarId) = prefs.edit { putInt(LAST_USED_CALDAV_CALENDAR, calendarId) }

    var lastUsedLocalCalendarId: Long
        get() = prefs.getLong(LAST_USED_LOCAL_CALENDAR_ID, LOCAL_CALENDAR_ID)
        set(lastUsedLocalCalendarId) = prefs.edit {
            putLong(LAST_USED_LOCAL_CALENDAR_ID, lastUsedLocalCalendarId)
        }

    var lastUsedIgnoreCalendarsState: Boolean
        get() = prefs.getBoolean(LAST_USED_IGNORE_CALENDARS_STATE, false)
        set(lastUsedIgnoreCalendarsState) = prefs.edit {
            putBoolean(LAST_USED_IGNORE_CALENDARS_STATE, lastUsedIgnoreCalendarsState)
        }

    var reminderAudioStream: Int
        get() = prefs.getInt(REMINDER_AUDIO_STREAM, AudioManager.STREAM_NOTIFICATION)
        set(reminderAudioStream) = prefs.edit { putInt(REMINDER_AUDIO_STREAM, reminderAudioStream) }

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit { putBoolean(REPLACE_DESCRIPTION, replaceDescription) }

    var displayDescription: Boolean
        get() = prefs.getBoolean(DISPLAY_DESCRIPTION, true)
        set(displayDescription) = prefs.edit { putBoolean(DISPLAY_DESCRIPTION, displayDescription) }

    var showGrid: Boolean
        get() = prefs.getBoolean(SHOW_GRID, false)
        set(showGrid) = prefs.edit { putBoolean(SHOW_GRID, showGrid) }

    var loopReminders: Boolean
        get() = prefs.getBoolean(LOOP_REMINDERS, false)
        set(loopReminders) = prefs.edit { putBoolean(LOOP_REMINDERS, loopReminders) }

    var dimPastEvents: Boolean
        get() = prefs.getBoolean(DIM_PAST_EVENTS, true)
        set(dimPastEvents) = prefs.edit { putBoolean(DIM_PAST_EVENTS, dimPastEvents) }

    var dimCompletedTasks: Boolean
        get() = prefs.getBoolean(DIM_COMPLETED_TASKS, true)
        set(dimCompletedTasks) = prefs.edit { putBoolean(DIM_COMPLETED_TASKS, dimCompletedTasks) }

    fun getSyncedCalendarIdsAsList() =
        caldavSyncedCalendarIds.split(",").filter { it.trim().isNotEmpty() }
            .map { Integer.parseInt(it) }.toMutableList() as ArrayList<Int>

    fun getDisplayCalendarsAsList() =
        displayCalendars.map { it.toLong() }.toMutableList() as ArrayList<Long>

    fun addDisplayCalendar(type: String) {
        addDisplayCalendars(HashSet(listOf(type)))
    }

    private fun addDisplayCalendars(types: Set<String>) {
        val currDisplayCalendars = HashSet(displayCalendars)
        currDisplayCalendars.addAll(types)
        displayCalendars = currDisplayCalendars
    }

    fun removeDisplayCalendars(types: Set<String>) {
        val currDisplayCalendars = HashSet(displayCalendars)
        currDisplayCalendars.removeAll(types)
        displayCalendars = currDisplayCalendars
    }

    var usePreviousEventReminders: Boolean
        get() = prefs.getBoolean(USE_PREVIOUS_EVENT_REMINDERS, true)
        set(usePreviousEventReminders) = prefs.edit { putBoolean(USE_PREVIOUS_EVENT_REMINDERS, usePreviousEventReminders) }

    var defaultReminder1: Int
        get() = prefs.getInt(DEFAULT_REMINDER_1, 10)
        set(defaultReminder1) = prefs.edit { putInt(DEFAULT_REMINDER_1, defaultReminder1) }

    var defaultReminder2: Int
        get() = prefs.getInt(DEFAULT_REMINDER_2, REMINDER_OFF)
        set(defaultReminder2) = prefs.edit { putInt(DEFAULT_REMINDER_2, defaultReminder2) }

    var defaultReminder3: Int
        get() = prefs.getInt(DEFAULT_REMINDER_3, REMINDER_OFF)
        set(defaultReminder3) = prefs.edit { putInt(DEFAULT_REMINDER_3, defaultReminder3) }

    var pullToRefresh: Boolean
        get() = prefs.getBoolean(PULL_TO_REFRESH, false)
        set(pullToRefresh) = prefs.edit { putBoolean(PULL_TO_REFRESH, pullToRefresh) }

    var lastVibrateOnReminder: Boolean
        get() = prefs.getBoolean(LAST_VIBRATE_ON_REMINDER, context.config.vibrateOnReminder)
        set(lastVibrateOnReminder) = prefs.edit { putBoolean(LAST_VIBRATE_ON_REMINDER, lastVibrateOnReminder) }

    var defaultStartTime: Int
        get() = prefs.getInt(DEFAULT_START_TIME, DEFAULT_START_TIME_NEXT_FULL_HOUR)
        set(defaultStartTime) = prefs.edit { putInt(DEFAULT_START_TIME, defaultStartTime) }

    var defaultDuration: Int
        get() = prefs.getInt(DEFAULT_DURATION, 0)
        set(defaultDuration) = prefs.edit { putInt(DEFAULT_DURATION, defaultDuration) }

    var defaultCalendarId: Long
        get() = prefs.getLong(DEFAULT_CALENDAR_ID, -1L)
        set(defaultCalendarId) = prefs.edit {
            putLong(DEFAULT_CALENDAR_ID, defaultCalendarId)
        }

    var allowChangingTimeZones: Boolean
        get() = prefs.getBoolean(ALLOW_CHANGING_TIME_ZONES, false)
        set(allowChangingTimeZones) =
            prefs.edit { putBoolean(ALLOW_CHANGING_TIME_ZONES, allowChangingTimeZones) }

    var addBirthdaysAutomatically: Boolean
        get() = prefs.getBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, false)
        set(addBirthdaysAutomatically) =
            prefs.edit { putBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, addBirthdaysAutomatically) }

    var addAnniversariesAutomatically: Boolean
        get() = prefs.getBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, false)
        set(addAnniversariesAutomatically) =
            prefs.edit { putBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, addAnniversariesAutomatically) }

    var birthdayReminders: ArrayList<Int>
        get() = prefs.getString(BIRTHDAY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",")
            .map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(birthdayReminders) =
            prefs.edit { putString(BIRTHDAY_REMINDERS, birthdayReminders.joinToString(",")) }

    var anniversaryReminders: ArrayList<Int>
        get() = prefs.getString(ANNIVERSARY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",")
            .map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(anniversaryReminders) =
            prefs.edit { putString(ANNIVERSARY_REMINDERS, anniversaryReminders.joinToString(",")) }

    var exportEvents: Boolean
        get() = prefs.getBoolean(EXPORT_EVENTS, true)
        set(exportEvents) = prefs.edit { putBoolean(EXPORT_EVENTS, exportEvents) }

    var exportTasks: Boolean
        get() = prefs.getBoolean(EXPORT_TASKS, true)
        set(exportTasks) = prefs.edit { putBoolean(EXPORT_TASKS, exportTasks) }

    var exportPastEntries: Boolean
        get() = prefs.getBoolean(EXPORT_PAST_EVENTS, true)
        set(exportPastEvents) = prefs.edit { putBoolean(EXPORT_PAST_EVENTS, exportPastEvents) }

    var weeklyViewItemHeightMultiplier: Float
        get() = prefs.getFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, 1f)
        set(weeklyViewItemHeightMultiplier) =
            prefs.edit { putFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, weeklyViewItemHeightMultiplier) }

    var weeklyViewDays: Int
        get() = prefs.getInt(WEEKLY_VIEW_DAYS, 7)
        set(weeklyViewDays) = prefs.edit { putInt(WEEKLY_VIEW_DAYS, weeklyViewDays) }

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(HIGHLIGHT_WEEKENDS, false)
        set(highlightWeekends) = prefs.edit { putBoolean(HIGHLIGHT_WEEKENDS, highlightWeekends) }

    var highlightWeekendsColor: Int
        get() = prefs.getInt(HIGHLIGHT_WEEKENDS_COLOR, context.resources.getColor(R.color.red_text))
        set(highlightWeekendsColor) = prefs.edit { putInt(HIGHLIGHT_WEEKENDS_COLOR, highlightWeekendsColor) }

    var lastUsedEventSpan: Int
        get() = prefs.getInt(LAST_USED_EVENT_SPAN, YEAR_SECONDS)
        set(lastUsedEventSpan) = prefs.edit { putInt(LAST_USED_EVENT_SPAN, lastUsedEventSpan) }

    var allowCreatingTasks: Boolean
        get() = prefs.getBoolean(ALLOW_CREATING_TASKS, true)
        set(allowCreatingTasks) = prefs.edit { putBoolean(ALLOW_CREATING_TASKS, allowCreatingTasks) }

    var wasFilteredOutWarningShown: Boolean
        get() = prefs.getBoolean(WAS_FILTERED_OUT_WARNING_SHOWN, false)
        set(wasFilteredOutWarningShown) =
            prefs.edit { putBoolean(WAS_FILTERED_OUT_WARNING_SHOWN, wasFilteredOutWarningShown) }

    var autoBackupCalendars: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_CALENDARS, HashSet())!!
        set(autoBackupCalendars) = prefs.edit {
            remove(AUTO_BACKUP_CALENDARS)
                .putStringSet(AUTO_BACKUP_CALENDARS, autoBackupCalendars)
        }

    var autoBackupEvents: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_EVENTS, true)
        set(autoBackupEvents) = prefs.edit { putBoolean(AUTO_BACKUP_EVENTS, autoBackupEvents) }

    var autoBackupTasks: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_TASKS, true)
        set(autoBackupTasks) = prefs.edit { putBoolean(AUTO_BACKUP_TASKS, autoBackupTasks) }

    var autoBackupPastEntries: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_PAST_ENTRIES, true)
        set(autoBackupPastEntries) =
            prefs.edit { putBoolean(AUTO_BACKUP_PAST_ENTRIES, autoBackupPastEntries) }

    var lastUsedShowListWidgetHeader: Boolean
        get() = prefs.getBoolean(LAST_USED_SHOW_LIST_WIDGET_HEADER, true)
        set(lastUsedShowListWidgetHeader) =
            prefs.edit { putBoolean(LAST_USED_SHOW_LIST_WIDGET_HEADER, lastUsedShowListWidgetHeader) }

    //Goodwy
    var widgetSecondTextColor: Int
        get() = prefs.getInt(
            WIDGET_SECOND_TEXT_COLOR,
            ContextCompat.getColor(context, com.goodwy.commons.R.color.theme_light_text_color)
        )
        set(widgetSecondTextColor) =
            prefs.edit { putInt(WIDGET_SECOND_TEXT_COLOR, widgetSecondTextColor) }

    var widgetMonthEventsVisibility: Int
        get() = prefs.getInt(WIDGET_MONTH_EVENTS_VISIBILITY, WIDGET_MONTH_EVENTS_INVISIBLE)
        set(widgetMonthEventsVisibility) =
            prefs.edit { putInt(WIDGET_MONTH_EVENTS_VISIBILITY, widgetMonthEventsVisibility) }

    var showWidgetName: Boolean
        get() = prefs.getBoolean(SHOW_WIDGET_NAME, true)
        set(showWidgetName) = prefs.edit { putBoolean(SHOW_WIDGET_NAME, showWidgetName) }

}
