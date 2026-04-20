package com.goodwy.calendar.dialogs

import android.app.Activity
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.goodwy.calendar.R
import com.goodwy.calendar.databinding.DialogCalendarBinding
import com.goodwy.calendar.extensions.calDAVHelper
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.calendar.helpers.OTHER_EVENT
import com.goodwy.calendar.models.CalendarEntity
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getProperBackgroundColor
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.commons.extensions.setFillWithStroke
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.showKeyboard
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.value
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.helpers.ensureBackgroundThread

class EditCalendarDialog(
    val activity: Activity,
    var calendar: CalendarEntity? = null,
    val callback: (calendar: CalendarEntity) -> Unit
) {
    private var isNewEvent = calendar == null
    private val binding by activity.viewBinding(DialogCalendarBinding::inflate)

    init {
        if (calendar == null) {
            calendar = CalendarEntity(null, "", activity.getProperPrimaryColor())
        }

        binding.apply {
            setupColor(typeColor)
            typeTitle.setText(calendar!!.title)
            typeColor.setOnClickListener {
                if (calendar?.caldavCalendarId == 0) {
                    ColorPickerDialog(
                        activity = activity,
                        color = calendar!!.color
                    ) { wasPositivePressed, color, _ ->
                        if (wasPositivePressed) {
                            calendar!!.color = color
                            setupColor(typeColor)
                        }
                    }
                } else {
                    val currentColor = calendar!!.color
                    val colors =
                        activity.calDAVHelper.getAvailableCalDAVCalendarColors(calendar!!).keys.toIntArray()
                    SelectCalendarColorDialog(
                        activity = activity,
                        colors = colors,
                        currentColor = currentColor
                    ) {
                        calendar!!.color = it
                        setupColor(typeColor)
                    }
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
                    titleId = if (isNewEvent) R.string.add_new_type else R.string.edit_type
                ) { alertDialog ->
                    alertDialog.showKeyboard(binding.typeTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        ensureBackgroundThread {
                            calendarConfirmed(binding.typeTitle.value, alertDialog)
                        }
                    }
                }
            }
    }

    private fun setupColor(view: ImageView) {
        view.setFillWithStroke(calendar!!.color, activity.getProperBackgroundColor())
    }

    private fun calendarConfirmed(title: String, dialog: AlertDialog) {
        val calendarClass = calendar?.type ?: OTHER_EVENT
        val calendarId = if (calendarClass == OTHER_EVENT) {
            activity.eventsHelper.getCalendarIdWithTitle(title)
        } else {
            activity.eventsHelper.getCalendarIdWithClass(calendarClass)
        }

        var isCalendarTitleTaken = isNewEvent && calendarId != -1L
        if (!isCalendarTitleTaken) {
            isCalendarTitleTaken = !isNewEvent && calendar!!.id != calendarId && calendarId != -1L
        }

        if (title.isEmpty()) {
            activity.toast(R.string.title_empty)
            return
        } else if (isCalendarTitleTaken) {
            activity.toast(R.string.type_already_exists)
            return
        }

        calendar!!.title = title
        if (calendar!!.caldavCalendarId != 0) {
            calendar!!.caldavDisplayName = title
        }

        calendar!!.id = activity.eventsHelper.insertOrUpdateCalendarSync(calendar!!)

        if (calendar!!.id != -1L) {
            activity.runOnUiThread {
                dialog.dismiss()
                callback(calendar!!)
            }
        } else {
            activity.toast(R.string.editing_calendar_failed)
        }
    }
}
