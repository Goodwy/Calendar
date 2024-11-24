package com.goodwy.calendar.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.goodwy.calendar.R
import com.goodwy.calendar.databinding.MonthViewBackgroundBinding
import com.goodwy.calendar.databinding.MonthViewBinding
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.launchNewEventIntent
import com.goodwy.calendar.extensions.launchNewTaskIntent
import com.goodwy.calendar.helpers.*
import com.goodwy.calendar.models.DayMonthly
import com.goodwy.commons.compose.extensions.getActivity
import com.goodwy.commons.dialogs.RadioGroupIconDialog
import com.goodwy.commons.extensions.onGlobalLayout
import com.goodwy.commons.models.RadioItem

// used in the Monthly view fragment, 1 view per screen
class MonthViewWrapper(context: Context, attrs: AttributeSet, defStyle: Int) : FrameLayout(context, attrs, defStyle) {
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var weekDaysLetterHeight = 0
    private var horizontalOffset = 0
    private var wereViewsAdded = false
    private var isMonthDayView = true
    private var days = ArrayList<DayMonthly>()
    private var inflater: LayoutInflater
    private var binding: MonthViewBinding
    private var dayClickCallback: ((day: DayMonthly) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        val normalTextSize = resources.getDimensionPixelSize(com.goodwy.commons.R.dimen.normal_text_size).toFloat()
        weekDaysLetterHeight = 2 * normalTextSize.toInt()

        inflater = LayoutInflater.from(context)
        binding = MonthViewBinding.inflate(inflater, this, true)
        setupHorizontalOffset()

        onGlobalLayout {
            if (!wereViewsAdded && days.isNotEmpty()) {
                measureSizes()
                addClickableBackgrounds()
                binding.monthView.updateDays(days, isMonthDayView)
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        measureSizes()
        var y = 0
        var x = 0
        var curLeft = dayWidth.toInt()
        val end = right + paddingRight

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is MonthView) {
                //ignore the MonthView layout
                continue
            }

            child.measure(
                MeasureSpec.makeMeasureSpec(dayWidth.toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dayHeight.toInt(), MeasureSpec.EXACTLY)
            )

            val childLeft = x * dayWidth + horizontalOffset - child.translationX
            val childTop = y * dayHeight + weekDaysLetterHeight - child.translationY
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            val childRight = childLeft + childWidth
            val childBottom = childTop + childHeight

            child.layout(childLeft.toInt(), childTop.toInt(), childRight.toInt(), childBottom.toInt())

            if (curLeft + childWidth <= end) {
                curLeft += childWidth
                x++
            } else {
                y++
                x = 0
                curLeft = childWidth
            }
        }
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, addEvents: Boolean, callback: ((DayMonthly) -> Unit)? = null) {
        setupHorizontalOffset()
        measureSizes()
        dayClickCallback = callback
        days = newDays
        if (dayWidth != 0f && dayHeight != 0f) {
            addClickableBackgrounds()
        }

        isMonthDayView = !addEvents
        binding.monthView.updateDays(days, isMonthDayView)
    }

    private fun setupHorizontalOffset() {
        horizontalOffset = if (context.config.showWeekNumbers) {
            resources.getDimensionPixelSize(com.goodwy.commons.R.dimen.smaller_text_size) * 2
        } else {
            0
        }
    }

    private fun measureSizes() {
        dayWidth = (width - horizontalOffset) / COLUMN_COUNT.toFloat()
        dayHeight = (height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
    }

    private fun addClickableBackgrounds() {
        removeAllViews()
        binding = MonthViewBinding.inflate(inflater, this, true)
        wereViewsAdded = true
        days.forEachIndexed { index, day ->
            addViewBackground(index % COLUMN_COUNT, index / COLUMN_COUNT, day)
        }

    }

    private fun addViewBackground(viewX: Int, viewY: Int, day: DayMonthly) {

        MonthViewBackgroundBinding.inflate(inflater, this, false).root.apply {
            if (isMonthDayView) {
                background = null
            }
            //Accessible label composed by day and month
            contentDescription = "${day.value} ${
                Formatter.getMonthName(
                    context, 
                    Formatter.getDateTimeFromCode(day.code).monthOfYear
                )
            }"

            setOnClickListener {
                dayClickCallback?.invoke(day)

                if (isMonthDayView) {
                    binding.monthView.updateCurrentlySelectedDay(viewX, viewY)
                }
            }

            setOnLongClickListener {
                if (context.config.allowCreatingTasks) {
                    val items = arrayListOf(
                        RadioItem(TYPE_EVENT, context.getString(R.string.event), icon = R.drawable.ic_today_vector),
                        RadioItem(TYPE_TASK, context.getString(R.string.task), icon = R.drawable.ic_task_vector)
                    )

                    RadioGroupIconDialog(context.getActivity(), items) {
                        if (it == TYPE_EVENT) {
                            context.launchNewEventIntent(day.code)
                        } else {
                            context.launchNewTaskIntent(day.code)
                        }
                    }
                } else {
                    context.launchNewEventIntent(day.code)
                }
                true
            }

            addView(this)
        }
    }

    fun togglePrintMode() {
        binding.monthView.togglePrintMode()
    }
}
