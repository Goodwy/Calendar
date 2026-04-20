package com.goodwy.calendar.activities

import android.os.Bundle
import com.goodwy.calendar.R
import com.goodwy.calendar.adapters.ManageCalendarsAdapter
import com.goodwy.calendar.databinding.ActivityManageCalendarsBinding
import com.goodwy.calendar.dialogs.EditCalendarDialog
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.calendar.interfaces.DeleteCalendarsListener
import com.goodwy.calendar.models.CalendarEntity
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.updateTextColors
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.helpers.NavigationIcon
import com.goodwy.commons.helpers.ensureBackgroundThread

class ManageCalendarsActivity : SimpleActivity(), DeleteCalendarsListener {

    private val binding by viewBinding(ActivityManageCalendarsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        setupEdgeToEdge(padBottomSystem = listOf(binding.manageCalendarsList))
        setupMaterialScrollListener(binding.manageCalendarsList, binding.manageCalendarsAppbar)

        getCalendars()
        updateTextColors(binding.manageCalendarsList)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.manageCalendarsAppbar, NavigationIcon.Arrow)
    }

    private fun showEditCalendarDialog(calendar: CalendarEntity? = null) {
        EditCalendarDialog(this, calendar?.copy()) {
            getCalendars()
        }
    }

    private fun getCalendars() {
        eventsHelper.getCalendars(this, false) {
            val adapter = ManageCalendarsAdapter(this, it, this, binding.manageCalendarsList) {
                showEditCalendarDialog(it as CalendarEntity)
            }
            binding.manageCalendarsList.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        binding.manageCalendarsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_calendar -> showEditCalendarDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun deleteCalendars(
        calendars: ArrayList<CalendarEntity>,
        deleteEvents: Boolean
    ): Boolean {
        if (calendars.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
            if (calendars.size == 1) {
                return false
            }
        }

        ensureBackgroundThread {
            eventsHelper.deleteCalendars(calendars, deleteEvents)
        }

        return true
    }
}
