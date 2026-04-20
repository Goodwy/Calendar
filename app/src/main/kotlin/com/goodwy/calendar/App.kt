package com.goodwy.calendar

import com.goodwy.calendar.extensions.hasDummyAlarm
import com.goodwy.calendar.jobs.AppStartupWorker
import com.goodwy.commons.RightApp
import com.goodwy.commons.helpers.PurchaseHelper

class App : RightApp() {
    override fun onCreate() {
        super.onCreate()
        if (!hasDummyAlarm()) {
            AppStartupWorker.start(this)
        }

        PurchaseHelper().initPurchaseIfNeed(this, "2063579402")
    }
}
