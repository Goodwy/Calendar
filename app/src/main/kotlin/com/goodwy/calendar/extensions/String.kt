package com.goodwy.calendar.extensions

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan

fun String.getMonthCode() = if (length == 8) substring(0, 6) else ""

fun String.toBold(): SpannableString {
    return SpannableString(this).apply {
        setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
