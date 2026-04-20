package com.goodwy.calendar.activities

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import com.goodwy.calendar.databinding.WidgetConfigDateBinding
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.helpers.Formatter
import com.goodwy.calendar.helpers.MyWidgetDateProvider
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.IS_CUSTOMIZING_COLORS

class WidgetDateConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColor = 0
    private var mSecondTextColor = 0
    private var mLabelColor = 0

    private val binding by viewBinding(WidgetConfigDateBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(binding.root)
        setupEdgeToEdge(padTopSystem = listOf(binding.configDateTimeHolder), padBottomSystem = listOf(binding.root))
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
            configWidgetNameTextColorHolder.setOnClickListener { pickLabelColor() }
            configBgSeekbar.setColors(mTextColor, primaryColor, primaryColor)
            widgetDateLabel.text = Formatter.getTodayDayNumber()
            widgetDayWeekLabel.text = Formatter.getCurrentMonthShort()
            widgetMonthLabel.text = Formatter.getCurrentMonthShort()

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
        mLabelColor = config.widgetLabelColor

        updateTextColor()
    }

    private fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()
        config.showWidgetName = binding.configWidgetName.isChecked

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColor
            widgetSecondTextColor = mSecondTextColor
            widgetLabelColor = mLabelColor
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency,
            addDefaultColorButton = true,
            colorDefault = resources.getColor(com.goodwy.commons.R.color.default_widget_bg_color)
        ) { wasPositivePressed, color, wasDefaultPressed ->
            if (wasPositivePressed || wasDefaultPressed) {
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
        ) { wasPositivePressed, color, wasDefaultPressed ->
            if (wasPositivePressed || wasDefaultPressed) {
                if (secondary) {
                    mSecondTextColor = color
                } else {
                    mTextColor = color
                }
                updateTextColor()
            }
        }
    }

    private fun pickLabelColor() {
        ColorPickerDialog(this, mLabelColor,
            addDefaultColorButton = true,
            colorDefault = resources.getColor(com.goodwy.commons.R.color.default_widget_label_color)
        ) { wasPositivePressed, color, wasDefaultPressed ->
            if (wasPositivePressed || wasDefaultPressed) {
                mLabelColor = color
                updateTextColor()
                handleWidgetNameDisplay()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetDateProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColor() {
        binding.apply {
            configTextColor.setFillWithStroke(mTextColor, mTextColor)
            configSecondaryTextColor.setFillWithStroke(mSecondTextColor, mSecondTextColor)
            configWidgetNameTextColor.setFillWithStroke(mLabelColor, mLabelColor)
            widgetDateLabel.setTextColor(mSecondTextColor)
            widgetDayWeekLabel.setTextColor(mTextColor)
            widgetMonthLabel.setTextColor(mSecondTextColor)
            widgetName.setTextColor(mLabelColor)
            configSave.setTextColor(getProperPrimaryColor().getContrastColor())
        }
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.apply {
            configDateTimeWrapper.background.applyColorFilter(mBgColor)
            configBgColor.setFillWithStroke(mBgColor, mBgColor)
            configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
        }
    }

    private fun handleWidgetNameDisplay() {
        val showName = binding.configWidgetName.isChecked
        binding.widgetName.beVisibleIf(showName)
        binding.configWidgetNameTextColorHolder.beVisibleIf(showName)
    }
}
