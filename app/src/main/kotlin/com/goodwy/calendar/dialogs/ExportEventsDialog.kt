package com.goodwy.calendar.dialogs

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.adapters.FilterCalendarAdapter
import com.goodwy.calendar.databinding.DialogExportEventsBinding
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.commons.dialogs.FilePickerDialog
import com.goodwy.commons.extensions.beGone
import com.goodwy.commons.extensions.beVisible
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getCurrentFormattedDateTime
import com.goodwy.commons.extensions.getParentPath
import com.goodwy.commons.extensions.hideKeyboard
import com.goodwy.commons.extensions.humanizePath
import com.goodwy.commons.extensions.internalStoragePath
import com.goodwy.commons.extensions.isAValidFilename
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.value
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.helpers.ensureBackgroundThread
import java.io.File

@SuppressLint("SetTextI18n")
class ExportEventsDialog(
    val activity: SimpleActivity,
    val path: String,
    val hidePath: Boolean,
    val callback: (file: File, calendars: ArrayList<Long>) -> Unit
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config
    private val binding by activity.viewBinding(DialogExportEventsBinding::inflate)

    init {
        binding.apply {
            exportEventsFolder.setText(activity.humanizePath(realPath))
            exportEventsFilename.setText("${activity.getString(R.string.events)}_${getCurrentFormattedDateTime()}")

            exportEventsCheckbox.isChecked = config.exportEvents
            exportEventsCheckboxHolder.setOnClickListener {
                exportEventsCheckbox.toggle()
            }
            exportTasksCheckbox.isChecked = config.exportTasks
            exportTasksCheckboxHolder.setOnClickListener {
                exportTasksCheckbox.toggle()
            }
            exportPastEventsCheckbox.isChecked = config.exportPastEntries
            exportPastEventsCheckboxHolder.setOnClickListener {
                exportPastEventsCheckbox.toggle()
            }

            if (hidePath) {
                exportEventsFolderHint.beGone()
                exportEventsFolder.beGone()
            } else {
                exportEventsFolder.setOnClickListener {
                    activity.hideKeyboard(exportEventsFilename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        exportEventsFolder.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }

            activity.eventsHelper.getCalendars(activity, false) {
                val calendars = HashSet<String>()
                it.mapTo(calendars) { it.id.toString() }

                exportEventsTypesList.adapter = FilterCalendarAdapter(activity, it, calendars)
                if (it.size > 1) {
                    exportEventsPickTypes.beVisible()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.export_events
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.exportEventsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(com.goodwy.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.ics")
                                if (!hidePath && file.exists()) {
                                    activity.toast(com.goodwy.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val exportEventsChecked = binding.exportEventsCheckbox.isChecked
                                val exportTasksChecked = binding.exportTasksCheckbox.isChecked
                                if (!exportEventsChecked && !exportTasksChecked) {
                                    activity.toast(com.goodwy.commons.R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.apply {
                                        lastExportPath = file.absolutePath.getParentPath()
                                        exportEvents = exportEventsChecked
                                        exportTasks = exportTasksChecked
                                        exportPastEntries =
                                            binding.exportPastEventsCheckbox.isChecked
                                    }

                                    val calendars =
                                        (binding.exportEventsTypesList.adapter as FilterCalendarAdapter).getSelectedItemsList()
                                    callback(file, calendars)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(com.goodwy.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
