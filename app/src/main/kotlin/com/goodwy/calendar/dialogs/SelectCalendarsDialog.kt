package com.goodwy.calendar.dialogs

import androidx.appcompat.app.AlertDialog
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.adapters.FilterCalendarAdapter
import com.goodwy.calendar.databinding.DialogFilterCalendarsBinding
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.viewBinding

class SelectCalendarsDialog(
    val activity: SimpleActivity,
    selectedCalendars: Set<String>,
    val callback: (HashSet<String>) -> Unit
) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogFilterCalendarsBinding::inflate)

    init {
        activity.eventsHelper.getCalendars(activity, false) {
            binding.filterCalendarsList.adapter =
                FilterCalendarAdapter(activity, it, selectedCalendars)

            activity.getAlertDialogBuilder()
                .setPositiveButton(com.goodwy.commons.R.string.ok) { _, _ -> confirmCalendars() }
                .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        dialog = alertDialog
                    }
                }
        }
    }

    private fun confirmCalendars() {
        val adapter = binding.filterCalendarsList.adapter as FilterCalendarAdapter
        val selectedItems = adapter.getSelectedItemsList()
            .map { it.toString() }
            .toHashSet()
        callback(selectedItems)
        dialog?.dismiss()
    }
}
