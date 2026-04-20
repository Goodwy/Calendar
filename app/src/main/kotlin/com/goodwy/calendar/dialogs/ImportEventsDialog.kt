package com.goodwy.calendar.dialogs

import androidx.appcompat.app.AlertDialog
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.databinding.DialogImportEventsBinding
import com.goodwy.calendar.extensions.calendarsDB
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.calendar.helpers.IcsImporter
import com.goodwy.calendar.helpers.IcsImporter.ImportResult.IMPORT_FAIL
import com.goodwy.calendar.helpers.IcsImporter.ImportResult.IMPORT_NOTHING_NEW
import com.goodwy.calendar.helpers.IcsImporter.ImportResult.IMPORT_OK
import com.goodwy.calendar.helpers.IcsImporter.ImportResult.IMPORT_PARTIAL
import com.goodwy.calendar.helpers.LOCAL_CALENDAR_ID
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getProperBackgroundColor
import com.goodwy.commons.extensions.setFillWithStroke
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.helpers.ensureBackgroundThread

class ImportEventsDialog(
    val activity: SimpleActivity,
    val path: String,
    val callback: (refreshView: Boolean) -> Unit
) {
    private var currCalendarId = LOCAL_CALENDAR_ID
    private var currCalendarCalDAVCalendarId = 0
    private val config = activity.config
    private val binding by activity.viewBinding(DialogImportEventsBinding::inflate)

    init {
        ensureBackgroundThread {
            if (activity.calendarsDB.getCalendarWithId(config.lastUsedLocalCalendarId) == null) {
                config.lastUsedLocalCalendarId = LOCAL_CALENDAR_ID
            }

            val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList()
                .contains(config.lastUsedCaldavCalendarId)
            currCalendarId = if (isLastCaldavCalendarOK) {
                val lastUsedCalDAVCalendar =
                    activity.eventsHelper.getCalendarWithCalDAVCalendarId(config.lastUsedCaldavCalendarId)
                if (lastUsedCalDAVCalendar != null) {
                    currCalendarCalDAVCalendarId = config.lastUsedCaldavCalendarId
                    lastUsedCalDAVCalendar.id!!
                } else {
                    LOCAL_CALENDAR_ID
                }
            } else {
                config.lastUsedLocalCalendarId
            }
            binding.importEventsCheckbox.isChecked = config.lastUsedIgnoreCalendarsState

            activity.runOnUiThread {
                initDialog()
            }
        }
    }

    private fun initDialog() {
        binding.apply {
            updateCalendar(this)
            importCalendarTitle.setOnClickListener {
                SelectCalendarDialog(
                    activity = activity,
                    currCalendar = currCalendarId,
                    showCalDAVCalendars = true,
                    showNewCalendarOption = true,
                    addLastUsedOneAsFirstOption = false,
                    showOnlyWritable = true,
                    showManageCalendars = false
                ) {
                    currCalendarId = it.id!!
                    currCalendarCalDAVCalendarId = it.caldavCalendarId

                    config.lastUsedLocalCalendarId = it.id!!
                    config.lastUsedCaldavCalendarId = it.caldavCalendarId

                    updateCalendar(this)
                }
            }

            importEventsCheckboxHolder.setOnClickListener {
                importEventsCheckbox.toggle()
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.import_events
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null)
                        activity.toast(com.goodwy.commons.R.string.importing)
                        ensureBackgroundThread {
                            val overrideFileCalendars = binding.importEventsCheckbox.isChecked
                            config.lastUsedIgnoreCalendarsState = overrideFileCalendars
                            val result = IcsImporter(activity).importEvents(
                                path = path,
                                defaultCalendarId = currCalendarId,
                                calDAVCalendarId = currCalendarCalDAVCalendarId,
                                overrideFileCalendars = overrideFileCalendars
                            )
                            handleParseResult(result)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun updateCalendar(binding: DialogImportEventsBinding) {
        ensureBackgroundThread {
            val calendar = activity.calendarsDB.getCalendarWithId(currCalendarId)
            activity.runOnUiThread {
                binding.importCalendarTitle.setText(calendar!!.getDisplayTitle())
                binding.importCalendarColor.setFillWithStroke(
                    calendar.color,
                    activity.getProperBackgroundColor()
                )
            }
        }
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        activity.toast(
            when (result) {
                IMPORT_NOTHING_NEW -> com.goodwy.commons.R.string.no_new_items
                IMPORT_OK -> com.goodwy.commons.R.string.importing_successful
                IMPORT_PARTIAL -> com.goodwy.commons.R.string.importing_some_entries_failed
                else -> com.goodwy.commons.R.string.no_items_found
            }
        )
        callback(result != IMPORT_FAIL)
    }
}
