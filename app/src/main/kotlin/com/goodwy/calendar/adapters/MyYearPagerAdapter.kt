package com.goodwy.calendar.adapters

import android.os.Bundle
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.goodwy.calendar.fragments.YearFragment
import com.goodwy.calendar.helpers.YEAR_LABEL
import com.goodwy.calendar.interfaces.NavigationListener

class MyYearPagerAdapter(fm: FragmentManager, val mYears: List<Int>, private val mListener: NavigationListener) : FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<YearFragment>()

    override fun getCount() = mYears.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val year = mYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle
        fragment.listener = mListener

        return fragment
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = super.instantiateItem(container, position)
        if (item is YearFragment) {
            mFragments.put(position, item)
        }
        return item
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        mFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun printCurrentView(pos: Int) {
        mFragments[pos].printCurrentView()
    }
}
