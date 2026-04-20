package com.goodwy.calendar.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.goodwy.calendar.R
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.isWeekendIndex
import com.goodwy.calendar.models.DayYearly
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.FontHelper
import com.goodwy.commons.helpers.MEDIUM_ALPHA

// used for displaying months at Yearly view
class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var todayCirclePaint: Paint
    private var dayWidth = 0f
    private var textColor = 0
    private var weekendsTextColor = 0
    private var days = 31
    private var isLandscape = false
    private var highlightWeekends = false
    private var isPrintVersion = false
    private var mEvents: ArrayList<DayYearly>? = null

    var firstDay = 0
    var todaysId = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setDays(days: Int) {
        this.days = days
        invalidate()
    }

    fun setEvents(events: ArrayList<DayYearly>?) {
        mEvents = events
        post { invalidate() }
    }

    init {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmallMonthView,
            0, 0
        )

        try {
            days = attributes.getInt(R.styleable.SmallMonthView_days, 31)
        } finally {
            attributes.recycle()
        }

        val baseColor = context.getProperTextColor()
        textColor = baseColor.adjustAlpha(MEDIUM_ALPHA)
        weekendsTextColor = context.config.highlightWeekendsColor.adjustAlpha(MEDIUM_ALPHA)
        highlightWeekends = context.config.highlightWeekends

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.year_view_day_text_size).toFloat()
            textAlign = Paint.Align.CENTER
            typeface = FontHelper.getTypeface(context)
        }

        todayCirclePaint = Paint(paint)
        todayCirclePaint.color = context.getProperPrimaryColor()//.adjustAlpha(MEDIUM_ALPHA)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dayWidth == 0f) {
            dayWidth = if (isLandscape) {
                width / 9f
            } else {
                width / 7f
            }
        }

        val fm = paint.fontMetrics
        val radius = dayWidth * 0.51f //0.41f

        var curId = 1 - firstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId in 1..days) {
                    val textPaint = getPaint(curId, x, highlightWeekends, curId == todaysId && !isPrintVersion)
                    val centerX = x * dayWidth - dayWidth / 2
                    val centerY = y * dayWidth - dayWidth / 2
                    val baselineY = centerY - (fm.ascent + fm.descent) / 2

                    if (curId == todaysId && !isPrintVersion) {
                        canvas.drawCircle(centerX, centerY, radius, todayCirclePaint)
                    }
                    canvas.drawText(curId.toString(), centerX, baselineY, textPaint)
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int, weekDay: Int, highlightWeekends: Boolean, isToday: Boolean): Paint {
        val colors = mEvents?.get(curId)?.eventColors ?: HashSet()
        if (isToday) {
            val curPaint = Paint(paint)
            curPaint.color = todayCirclePaint.color.getContrastColor()
            return curPaint
        } else if (colors.isNotEmpty()) {
            val curPaint = Paint(paint)
            curPaint.color = colors.first()
            return curPaint
        } else if (highlightWeekends && context.isWeekendIndex(weekDay - 1)) {
            val curPaint = Paint(paint)
            curPaint.color = weekendsTextColor
            return curPaint
        }

        return paint
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(com.goodwy.commons.R.color.theme_light_text_color, null)
        } else {
            context.getProperTextColor().adjustAlpha(MEDIUM_ALPHA)
        }

        paint.color = textColor
        invalidate()
    }
}

