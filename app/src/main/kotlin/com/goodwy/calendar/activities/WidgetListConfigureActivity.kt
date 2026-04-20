package com.goodwy.calendar.activities

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import com.goodwy.calendar.R
import com.goodwy.calendar.adapters.EventListAdapter
import com.goodwy.calendar.databinding.WidgetConfigListBinding
import com.goodwy.calendar.dialogs.CustomPeriodPickerDialog
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.seconds
import com.goodwy.calendar.extensions.widgetsDB
import com.goodwy.calendar.helpers.EVENT_PERIOD_CUSTOM
import com.goodwy.calendar.helpers.EVENT_PERIOD_TODAY
import com.goodwy.calendar.helpers.Formatter
import com.goodwy.calendar.helpers.MyWidgetListProvider
import com.goodwy.calendar.models.ListEvent
import com.goodwy.calendar.models.ListItem
import com.goodwy.calendar.models.ListSectionDay
import com.goodwy.calendar.models.Widget
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.dialogs.RadioGroupDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.*
import com.goodwy.commons.models.RadioItem
import org.joda.time.DateTime
import java.util.TreeSet

class WidgetListConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColor = 0
    private var mSecondTextColor = 0
    private var mLabelColor = 0
    private var mSelectedPeriodOption = 0

    private val binding by viewBinding(WidgetConfigListBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(binding.root)
        setupEdgeToEdge(padTopSystem = listOf(binding.configListHolder), padBottomSystem = listOf(binding.root))
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        binding.apply {
            EventListAdapter(this@WidgetListConfigureActivity, getListItems(), false, true, null, configWidgetPreview.configEventsList) {}.apply {
                updateTextColor(mSecondTextColor)
                configWidgetPreview.configEventsList.adapter = this
            }

            updateTextColors(configHolder)
            configHolder.background.applyColorFilter(getProperBackgroundColor())

            periodPickerHolder.setOnClickListener { showPeriodSelector() }

            configSave.setOnClickListener { saveConfig() }
            configBgColorHolder.setOnClickListener { pickBackgroundColor() }
            configTextColorHolder.setOnClickListener { pickTextColor() }
            configSecondaryTextColorHolder.setOnClickListener { pickTextColor(true) }
            configWidgetNameTextColorHolder.setOnClickListener { pickLabelColor() }

//            periodPickerHolder.beGoneIf(isCustomizingColors)

            val primaryColor = getProperPrimaryColor()
            configBgSeekbar.setColors(mTextColor, primaryColor, primaryColor)

            configWidgetName.isChecked = config.showWidgetName
            handleWidgetNameDisplay()
            configWidgetNameHolder.setOnClickListener {
                configWidgetName.toggle()
                handleWidgetNameDisplay()
            }
        }

        updateSelectedPeriod(config.lastUsedEventSpan)

        binding.showWidgetHeader.isChecked = config.lastUsedShowListWidgetHeader
        binding.configTextColorHolder.beVisibleIf(config.lastUsedShowListWidgetHeader)
        binding.configWidgetPreview.widgetHeaderInclude.widgetHeader.beVisibleIf(config.lastUsedShowListWidgetHeader)

        binding.showWidgetHeaderHolder.setOnClickListener {
            binding.showWidgetHeader.toggle()
            binding.configTextColorHolder.beVisibleIf(binding.showWidgetHeader.isChecked)
            binding.configWidgetPreview.widgetHeaderInclude.widgetHeader.beVisibleIf(binding.showWidgetHeader.isChecked)
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
        val widget = Widget(null, mWidgetId, mSelectedPeriodOption, binding.showWidgetHeader.isChecked)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()
        config.showWidgetName = binding.configWidgetName.isChecked

        config.lastUsedEventSpan = mSelectedPeriodOption
        config.lastUsedShowListWidgetHeader = binding.showWidgetHeader.isChecked

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(RESULT_OK, this)
        }
        finish()
    }

    private fun showPeriodSelector() {
        hideKeyboard()
        val seconds = TreeSet<Int>()
        seconds.apply {
            add(EVENT_PERIOD_TODAY)
            add(WEEK_SECONDS)
            add(MONTH_SECONDS)
            add(YEAR_SECONDS)
            add(mSelectedPeriodOption)
        }

        val items = ArrayList<RadioItem>(seconds.size)
        seconds.mapIndexedTo(items) { index, value ->
            RadioItem(index, getFormattedSeconds(value), value)
        }

        var selectedIndex = 0
        seconds.forEachIndexed { index, value ->
            if (value == mSelectedPeriodOption) {
                selectedIndex = index
            }
        }

        items.add(RadioItem(EVENT_PERIOD_CUSTOM, getString(R.string.within_the_next)))

        RadioGroupDialog(this, items, selectedIndex, showOKButton = true, cancelCallback = null) {
            val option = it as Int
            if (option == EVENT_PERIOD_CUSTOM) {
                CustomPeriodPickerDialog(this) {
                    updateSelectedPeriod(it)
                }
            } else {
                updateSelectedPeriod(option)
            }
        }
    }

    private fun updateSelectedPeriod(selectedPeriod: Int) {
        mSelectedPeriodOption = selectedPeriod
        when (selectedPeriod) {
            0 -> {
                mSelectedPeriodOption = YEAR_SECONDS
                binding.periodPickerValue.setText(R.string.within_the_next_one_year)
            }

            EVENT_PERIOD_TODAY -> binding.periodPickerValue.setText(R.string.today_only)
            else -> binding.periodPickerValue.text = getFormattedSeconds(mSelectedPeriodOption)
        }
    }

    private fun getFormattedSeconds(seconds: Int): String = if (seconds == EVENT_PERIOD_TODAY) {
        getString(R.string.today_only)
    } else {
        when {
            seconds == YEAR_SECONDS -> getString(R.string.within_the_next_one_year)
            seconds % MONTH_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_months, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            seconds % WEEK_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_weeks, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            else -> resources.getQuantityString(R.plurals.within_the_next_days, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
        }
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
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetListProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColor() {
        binding.configWidgetPreview.widgetHeaderInclude.widgetEventListToday.setTextColor(mTextColor)
        binding.configWidgetPreview.widgetHeaderInclude.widgetEventGoToToday.setColorFilter(mTextColor)
        binding.configWidgetPreview.widgetHeaderInclude.widgetEventNewEvent.setColorFilter(mTextColor)
        (binding.configWidgetPreview.configEventsList.adapter as? EventListAdapter)?.updateTextColor(mSecondTextColor)

        binding.configTextColor.setFillWithStroke(mTextColor, mTextColor)
        binding.configSecondaryTextColor.setFillWithStroke(mSecondTextColor, mSecondTextColor)
        binding.configWidgetNameTextColor.setFillWithStroke(mLabelColor, mLabelColor)
        binding.configSave.setTextColor(getProperPrimaryColor().getContrastColor())
        binding.widgetName.setTextColor(mLabelColor)
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.configWidgetPreview.widgetConfigEventListBackground.setColorFilter(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, mBgColor)
        binding.configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS(dateTime.seconds())
        var day = Formatter.getDayOfWeekTitle(code) + " " + Formatter.getDateFromCode(this, code, true, true)
        listItems.add(ListSectionDay(resources.getString(com.goodwy.commons.R.string.today), code, true, false))

        var time = dateTime.withHourOfDay(7)
        listItems.add(
            ListEvent.empty.copy(
                id = 1,
                startTS = time.seconds(),
                endTS = time.plusMinutes(105).seconds(),
                title = getString(R.string.sample_title_1),
                description = getString(R.string.sample_description_1),
                color = getProperPrimaryColor(),
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDayOfWeekTitle(code) + " " + Formatter.getDateFromCode(this, code, true, true)
        listItems.add(ListSectionDay(day, code, false, false))

        time = dateTime.withHourOfDay(9)
        listItems.add(
            ListEvent.empty.copy(
                id = 2,
                startTS = time.seconds(),
                endTS = time.seconds(),
                title = getString(R.string.sample_title_2),
                description = getString(R.string.sample_description_2),
                color = 0xFFD255F1.toInt(),
                isTask = true
            )
        )

        time = dateTime.withHourOfDay(11)
        listItems.add(
            ListEvent.empty.copy(
                id = 3,
                startTS = time.seconds(),
                endTS = time.seconds(),
                title = getString(R.string.sample_title_5),
                description = "",
                color = 0xFFF5C522.toInt(),
                isTask = true
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDayOfWeekTitle(code) + " " + Formatter.getDateFromCode(this, code, true, true)
        listItems.add(ListSectionDay(day, code, false, false))

        time = dateTime.withHourOfDay(12)
        listItems.add(
            ListEvent.empty.copy(
                id = 3,
                startTS = time.seconds(),
                endTS = time.plusHours(1).seconds(),
                title = getString(R.string.sample_title_3),
                description = "",
                isAllDay = true,
                color = 0xFFFA6A21.toInt(),
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDayOfWeekTitle(code) + " " + Formatter.getDateFromCode(this, code, true, true)
        listItems.add(ListSectionDay(day, code, false, false))

        time = dateTime.withHourOfDay(7)
        listItems.add(
            ListEvent.empty.copy(
                id = 1,
                startTS = time.seconds(),
                endTS = time.plusMinutes(105).seconds(),
                title = getString(R.string.sample_title_1),
                description = getString(R.string.sample_description_1),
                color = getProperPrimaryColor(),
            )
        )

        time = dateTime.withHourOfDay(13)
        listItems.add(
            ListEvent.empty.copy(
                id = 2,
                startTS = time.seconds(),
                endTS = time.seconds(),
                title = getString(R.string.sample_title_4),
                description = getString(R.string.sample_description_4),
                color = 0xFFD255F1.toInt(),
                isTask = true
            )
        )

        return listItems
    }

    private fun handleWidgetNameDisplay() {
        val showName = binding.configWidgetName.isChecked
        binding.widgetName.beVisibleIf(showName)
        binding.configWidgetNameTextColorHolder.beVisibleIf(showName)
    }
}
