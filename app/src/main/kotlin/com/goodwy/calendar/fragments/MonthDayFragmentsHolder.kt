package com.goodwy.calendar.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.goodwy.calendar.activities.MainActivity
import com.goodwy.calendar.adapters.MyMonthDayPagerAdapter
import com.goodwy.calendar.databinding.FragmentMonthsDaysHolderBinding
import com.goodwy.calendar.extensions.getMonthCode
import com.goodwy.calendar.helpers.DAY_CODE
import com.goodwy.calendar.helpers.Formatter
import com.goodwy.calendar.helpers.MONTHLY_DAILY_VIEW
import com.goodwy.calendar.interfaces.NavigationListener
import com.goodwy.commons.extensions.beGone
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getProperBackgroundColor
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.views.MyViewPager
import org.joda.time.DateTime

class MonthDayFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private lateinit var viewPager: MyViewPager
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false

    override val viewType = MONTHLY_DAILY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentMonthsDaysHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentMonthsDaysViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val codes = getMonths(currentDayCode)
        val monthlyDailyAdapter = MyMonthDayPagerAdapter(requireActivity().supportFragmentManager, codes, this)
        defaultMonthlyPage = codes.size / 2

        viewPager.apply {
            adapter = monthlyDailyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultMonthlyPage
        }
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code).withDayOfMonth(1)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = getCurrentDate()!!
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .setPositiveButton(com.goodwy.commons.R.string.ok) { _, _ -> datePicked(dateTime, datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyMonthDayPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode.getMonthCode() != todayDayCode.getMonthCode()

    override fun getNewEventDayCode() = (viewPager.adapter as? MyMonthDayPagerAdapter)?.getNewEventDayCode(viewPager.currentItem)
        ?: if (shouldGoToTodayBeVisible()) {
            currentDayCode
        } else {
            todayDayCode
        }

    override fun printView() {}

    override fun getCurrentDate(): DateTime? {
        return if (currentDayCode != "") {
            Formatter.getLocalDateTimeFromCode(currentDayCode)
        } else {
            null
        }
    }
}
