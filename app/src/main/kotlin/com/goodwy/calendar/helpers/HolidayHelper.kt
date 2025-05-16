package com.goodwy.calendar.helpers

import android.content.Context
import com.google.gson.Gson
import com.goodwy.calendar.models.HolidayInfo

class HolidayHelper(private val context: Context) {

    companion object {
        private const val HOLIDAY_METADATA = "holidays/metadata.json"
    }

    fun load(): List<HolidayInfo> {
        val metadata = context.assets.open(HOLIDAY_METADATA)
            .bufferedReader()
            .use { it.readText() }
        return Gson().fromJson(metadata, Array<HolidayInfo>::class.java)
            .toList()
            .sortedBy { it.country }
    }
}
