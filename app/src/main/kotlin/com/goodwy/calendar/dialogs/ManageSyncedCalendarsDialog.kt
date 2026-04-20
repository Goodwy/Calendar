package com.goodwy.calendar.dialogs

import android.text.TextUtils
import android.widget.RelativeLayout
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.databinding.CalendarItemAccountBinding
import com.goodwy.calendar.databinding.CalendarItemCalendarBinding
import com.goodwy.calendar.databinding.DialogSelectCalendarsBinding
import com.goodwy.calendar.extensions.calDAVHelper
import com.goodwy.calendar.extensions.config
import com.goodwy.commons.extensions.beVisibleIf
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.views.MyAppCompatCheckbox

class ManageSyncedCalendarsDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var prevAccount = ""
    private val binding by activity.viewBinding(DialogSelectCalendarsBinding::inflate)

    init {
        val ids = activity.config.getSyncedCalendarIdsAsList()
        val calendars = activity.calDAVHelper.getCalDAVCalendars("", true)
        binding.apply {
            dialogSelectCalendarsPlaceholder.beVisibleIf(calendars.isEmpty())
            dialogSelectCalendarsHolder.beVisibleIf(calendars.isNotEmpty())
        }

        val sorted = calendars.sortedWith(compareBy({ it.accountName }, { it.displayName }))
        sorted.forEach {
            if (prevAccount != it.accountName) {
                prevAccount = it.accountName
                addCalendarItem(false, it.accountName)
            }

            addCalendarItem(true, it.displayName, it.id, ids.contains(it.id))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok) { _, _ -> confirmSelection() }
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.select_caldav_calendars)
            }
    }

    private fun addCalendarItem(
        isEvent: Boolean,
        text: String,
        tag: Int = 0,
        shouldCheck: Boolean = false
    ) {
        val itemBinding = if (isEvent) {
            CalendarItemCalendarBinding.inflate(
                activity.layoutInflater,
                binding.dialogSelectCalendarsHolder,
                false
            ).apply {
                calendarItemCalendarSwitch.tag = tag
                calendarItemCalendarSwitch.text = text
                calendarItemCalendarSwitch.isChecked = shouldCheck
                root.setOnClickListener {
                    calendarItemCalendarSwitch.toggle()
                }
            }
        } else {
            CalendarItemAccountBinding.inflate(
                activity.layoutInflater,
                binding.dialogSelectCalendarsHolder,
                false
            ).apply {
                calendarItemAccount.text = text
            }
        }

        binding.dialogSelectCalendarsHolder.addView(itemBinding.root)
    }

    private fun confirmSelection() {
        val calendarIds = ArrayList<Int>()
        val childCnt = binding.dialogSelectCalendarsHolder.childCount
        for (i in 0..childCnt) {
            val child = binding.dialogSelectCalendarsHolder.getChildAt(i)
            if (child is RelativeLayout) {
                val check = child.getChildAt(0)
                if (check is MyAppCompatCheckbox && check.isChecked) {
                    calendarIds.add(check.tag as Int)
                }
            }
        }

        activity.config.caldavSyncedCalendarIds = TextUtils.join(",", calendarIds)
        callback()
    }
}
