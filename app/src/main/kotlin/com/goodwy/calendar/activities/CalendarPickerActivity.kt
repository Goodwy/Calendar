package com.goodwy.calendar.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.goodwy.calendar.R
import com.goodwy.calendar.extensions.launchNewEventIntent
import com.goodwy.calendar.extensions.launchNewTaskIntent
import com.goodwy.commons.dialogs.RadioGroupDialog
import com.goodwy.commons.models.RadioItem

class CalendarPickerActivity : AppCompatActivity() {
    private val TYPE_EVENT = 0
    private val TYPE_TASK = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val items = arrayListOf(
            RadioItem(TYPE_EVENT, getString(R.string.event)),
            RadioItem(TYPE_TASK, getString(R.string.task))
        )
        RadioGroupDialog(this, items = items, cancelCallback = { dialogCancelled() }) {
            val checkedId = it as Int
            if (checkedId == TYPE_EVENT) {
                launchNewEventIntent()
            } else if (checkedId == TYPE_TASK) {
                launchNewTaskIntent()
            }
            finish()
        }
    }

    private fun dialogCancelled() {
        finish()
    }
}
