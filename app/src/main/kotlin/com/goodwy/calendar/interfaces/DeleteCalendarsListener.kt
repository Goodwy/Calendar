package com.goodwy.calendar.interfaces

import com.goodwy.calendar.models.CalendarEntity

interface DeleteCalendarsListener {
    fun deleteCalendars(calendars: ArrayList<CalendarEntity>, deleteEvents: Boolean): Boolean
}
