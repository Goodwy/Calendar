package com.goodwy.calendar.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.goodwy.calendar.databinding.DayMonthlyNumberViewBinding
import com.goodwy.calendar.databinding.WidgetConfigMonthlyBinding
import com.goodwy.calendar.extensions.addDayEvents
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.isWeekendIndex
import com.goodwy.calendar.helpers.MonthlyCalendarImpl
import com.goodwy.calendar.helpers.MyWidgetMonthlyProvider
import com.goodwy.calendar.interfaces.MonthlyCalendar
import com.goodwy.calendar.models.DayMonthly
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.IS_CUSTOMIZING_COLORS
import com.goodwy.commons.helpers.LOWER_ALPHA
import org.joda.time.DateTime

class WidgetMonthlyConfigureActivity : SimpleActivity(), MonthlyCalendar {
    private var mDays: List<DayMonthly>? = null

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColor = 0
    private var mSecondTextColor = 0

    private val binding by viewBinding(WidgetConfigMonthlyBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(binding.root)
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        val primaryColor = getProperPrimaryColor()
        binding.apply {
            updateTextColors(configHolder)
            configHolder.background.applyColorFilter(getProperBackgroundColor())

            configSave.setOnClickListener { saveConfig() }
            configBgColorHolder.setOnClickListener { pickBackgroundColor() }
            configTextColorHolder.setOnClickListener { pickTextColor() }
            configSecondaryTextColorHolder.setOnClickListener { pickTextColor(true) }
            configBgSeekbar.setColors(mTextColor, primaryColor, primaryColor)

            configWidgetName.isChecked = config.showWidgetName
            handleWidgetNameDisplay()
            configWidgetNameHolder.setOnClickListener {
                configWidgetName.toggle()
                handleWidgetNameDisplay()
            }
        }
    }

    private fun initVariables() {
        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255f

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        binding.configBgSeekbar.apply {
            progress = (mBgAlpha * 100).toInt()

            onSeekBarChangeListener { progress ->
                mBgAlpha = progress / 100f
                updateBackgroundColor()
            }
        }
        updateBackgroundColor()

        mTextColor = config.widgetTextColor
        if (mTextColor == resources.getColor(com.goodwy.commons.R.color.default_widget_text_color) && isDynamicTheme()) {
            mTextColor = resources.getColor(com.goodwy.commons.R.color.you_primary_color, theme)
        }
        mSecondTextColor = config.widgetSecondTextColor

        updateTextColor()

        MonthlyCalendarImpl(this, this).updateMonthlyCalendar(DateTime().withDayOfMonth(1))
    }

    private fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()
        config.showWidgetName = binding.configWidgetName.isChecked

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColor
            widgetSecondTextColor = mSecondTextColor
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency,
            addDefaultColorButton = true,
            colorDefault = resources.getColor(com.goodwy.commons.R.color.default_widget_bg_color)
        ) { wasPositivePressed, color, _ ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBackgroundColor()
            }
        }
    }

    private fun pickTextColor(secondary: Boolean = false) {
        ColorPickerDialog(this, if (secondary) mSecondTextColor else mTextColor,
            addDefaultColorButton = true,
            colorDefault = if (secondary) resources.getColor(com.goodwy.commons.R.color.theme_light_text_color)
            else resources.getColor(com.goodwy.commons.R.color.default_widget_text_color)
        ) { wasPositivePressed, color, _ ->
            if (wasPositivePressed) {
                if (secondary) {
                    mSecondTextColor = color
                } else {
                    mTextColor = color
                }
                updateTextColor()
                updateDays()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetMonthlyProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColor() {
//        topNavigationBinding.topLeftArrow.applyColorFilter(mTextColor)
//        topNavigationBinding.topRightArrow.applyColorFilter(mTextColor)
        binding.configCalendar.topValue.setTextColor(mTextColor)
        binding.configCalendar.topNewEvent.applyColorFilter(mTextColor)
        binding.configTextColor.setFillWithStroke(mTextColor, mTextColor)
        binding.configSecondaryTextColor.setFillWithStroke(mSecondTextColor, mSecondTextColor)
        binding.configSave.setTextColor(getProperPrimaryColor().getContrastColor())
        updateLabels()
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.configCalendar.widgetMonthBackground.applyColorFilter(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, mBgColor)
        binding.configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun updateDays() {
        val daysLength = mDays!!.size
        binding.configCalendar.apply {
            if (config.showWeekNumbers) {
                firstRow.weekNum.setTextColor(mSecondTextColor)
                firstRow.weekNum.beVisible()

                arrayOf(weekNum0, weekNum1, weekNum2, weekNum3, weekNum4, weekNum5).forEachIndexed { index, textView ->
                    textView.apply {
                        @SuppressLint("SetTextI18n")
                        text = "${mDays!![index * 7 + 3].weekOfYear}:"
                        setTextColor(mSecondTextColor)
                        beVisible()
                    }
                }
            }

            //val dividerMargin = resources.displayMetrics.density.toInt()
            val dayViews = arrayOf(
                day0, day1, day2, day3, day4, day5, day6, day7, day8, day9, day10, day11, day12, day13,
                day14, day15, day16, day17, day18, day19, day20, day21, day22, day23, day24, day25, day26, day27,
                day28, day29, day30, day31, day32, day33, day34, day35, day36, day37, day38, day39, day40, day41
            )

            for (i in 0 until daysLength) {
                val day = mDays!![i]
                val dayTextColor = if (config.highlightWeekends && day.isWeekend) {
                    config.highlightWeekendsColor
                } else {
                    mSecondTextColor
                }

                dayViews[i].apply {
                    removeAllViews()
                    addDayNumber(dayTextColor, day, this)
                    //context.addDayEvents(day, this, resources, dividerMargin)
                }
            }
        }
    }

    private fun addDayNumber(rawTextColor: Int, day: DayMonthly, linearLayout: LinearLayout) {
        var textColor = rawTextColor
        if (!day.isThisMonth) {
            textColor = textColor.adjustAlpha(LOWER_ALPHA)
        }

        DayMonthlyNumberViewBinding.inflate(layoutInflater).apply {
            root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            linearLayout.addView(root)

            dayMonthlyNumberBackground.beVisibleIf(day.isToday)
            dayMonthlyNumberId.apply {
                setTextColor(textColor)
                text = day.value.toString()
                gravity = Gravity.CENTER// or Gravity.CENTER_HORIZONTAL
            }

            if (day.isToday) {
                dayMonthlyNumberBackground.setColorFilter(mTextColor)
                dayMonthlyNumberId.setTextColor(mTextColor.getContrastColor())
            }
        }
    }

    override fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>, checkedEvents: Boolean, currTargetDate: DateTime) {
        runOnUiThread {
            mDays = days
            binding.configCalendar.topValue.text = month
            updateDays()
        }
    }

    private fun updateLabels() {
        val weekendsTextColor = config.highlightWeekendsColor
        binding.configCalendar.firstRow.apply {
            arrayOf(label0, label1, label2, label3, label4, label5, label6).forEachIndexed { index, textView ->
                val textColor = if (config.highlightWeekends && isWeekendIndex(index)) {
                    weekendsTextColor
                } else {
                    mSecondTextColor
                }

                textView.setTextColor(textColor)
            }
        }
    }

    private fun handleWidgetNameDisplay() {
        val showName = binding.configWidgetName.isChecked
        binding.configCalendar.widgetName.beVisibleIf(showName)
    }
}
