package com.goodwy.calendar.extensions

import android.app.Activity
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import com.goodwy.calendar.BuildConfig
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.dialogs.CustomEventRepeatIntervalDialog
import com.goodwy.calendar.dialogs.ImportEventsDialog
import com.goodwy.calendar.helpers.*
import com.goodwy.calendar.models.Event
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.NewAppDialog
import com.goodwy.commons.dialogs.RadioGroupDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.LICENSE_JODA
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.commons.models.FAQItem
import com.goodwy.commons.models.RadioItem
import java.io.File
import java.io.FileOutputStream
import java.util.TreeSet
import kotlin.ranges.random

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(com.goodwy.commons.R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsOrTasksWithIds(ids) as ArrayList<Event>
        if (events.isEmpty()) {
            toast(com.goodwy.commons.R.string.no_items_found)
        }

        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter(this).exportEvents(it, events, false) { result ->
                if (result == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(com.goodwy.commons.R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(YEAR)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    items.add(RadioItem(-1, getString(com.goodwy.commons.R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) {
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}

fun SimpleActivity.tryImportEventsFromFile(uri: Uri, callback: (Boolean) -> Unit = {}) {
    when (uri.scheme) {
        "file" -> showImportEventsDialog(uri.path!!, callback)
        "content" -> {
            val tempFile = getTempFile()
            if (tempFile == null) {
                toast(com.goodwy.commons.R.string.unknown_error_occurred)
                return
            }

            try {
                val inputStream = contentResolver.openInputStream(uri)
                val out = FileOutputStream(tempFile)
                inputStream!!.copyTo(out)
                showImportEventsDialog(tempFile.absolutePath, callback)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }

        else -> toast(com.goodwy.commons.R.string.invalid_file_format)
    }
}

fun SimpleActivity.showImportEventsDialog(path: String, callback: (Boolean) -> Unit) {
    ImportEventsDialog(this, path, callback)
}

fun SimpleActivity.launchAbout() {
    val licenses = LICENSE_JODA

    val faqItems = arrayListOf(
        FAQItem(
            title = "${getString(R.string.faq_2_title)} ${getString(R.string.faq_2_title_extra)}",
            text = R.string.faq_2_text
        ),
        FAQItem(R.string.faq_5_title, R.string.faq_5_text),
        FAQItem(R.string.faq_3_title, R.string.faq_3_text),
        FAQItem(R.string.faq_6_title, R.string.faq_6_text),
        FAQItem(R.string.faq_1_title, R.string.faq_1_text),
        FAQItem(
            title = com.goodwy.commons.R.string.faq_1_title_commons,
            text = com.goodwy.commons.R.string.faq_1_text_commons
        ),
        FAQItem(
            title = com.goodwy.commons.R.string.faq_4_title_commons,
            text = com.goodwy.commons.R.string.faq_4_text_commons
        ),
        FAQItem(R.string.faq_4_title, R.string.faq_4_text)
    )

    if (!resources.getBoolean(com.goodwy.commons.R.bool.hide_google_relations)) {
        faqItems.add(
            FAQItem(
                title = com.goodwy.commons.R.string.faq_2_title_commons,
                text = com.goodwy.strings.R.string.faq_2_text_commons_g
            )
        )
        faqItems.add(
            FAQItem(
                title = com.goodwy.commons.R.string.faq_6_title_commons,
                text = com.goodwy.strings.R.string.faq_6_text_commons_g
            )
        )
        faqItems.add(
            FAQItem(
                title = com.goodwy.commons.R.string.faq_7_title_commons,
                text = com.goodwy.commons.R.string.faq_7_text_commons
            )
        )
    }

    val productIdX1 = BuildConfig.PRODUCT_ID_X1
    val productIdX2 = BuildConfig.PRODUCT_ID_X2
    val productIdX3 = BuildConfig.PRODUCT_ID_X3
    val subscriptionIdX1 = BuildConfig.SUBSCRIPTION_ID_X1
    val subscriptionIdX2 = BuildConfig.SUBSCRIPTION_ID_X2
    val subscriptionIdX3 = BuildConfig.SUBSCRIPTION_ID_X3
    val subscriptionYearIdX1 = BuildConfig.SUBSCRIPTION_YEAR_ID_X1
    val subscriptionYearIdX2 = BuildConfig.SUBSCRIPTION_YEAR_ID_X2
    val subscriptionYearIdX3 = BuildConfig.SUBSCRIPTION_YEAR_ID_X3

    val flavorName = BuildConfig.FLAVOR
    val storeDisplayName = when (flavorName) {
        "gplay" -> "Google Play"
        "foss" -> "FOSS"
        "rustore" -> "RuStore"
        else -> ""
    }
    val versionName = BuildConfig.VERSION_NAME
    val fullVersionText = "$versionName ($storeDisplayName)"

    startAboutActivity(
        appNameId = R.string.app_name_g,
        licenseMask = licenses,
        versionName = fullVersionText,
        flavorName = BuildConfig.FLAVOR,
        faqItems = faqItems,
        showFAQBeforeMail = true,
        productIdList= arrayListOf(productIdX1, productIdX2, productIdX3),
        productIdListRu = arrayListOf(productIdX1, productIdX2, productIdX3),
        subscriptionIdList = arrayListOf(subscriptionIdX1, subscriptionIdX2, subscriptionIdX3),
        subscriptionIdListRu = arrayListOf(subscriptionIdX1, subscriptionIdX2, subscriptionIdX3),
        subscriptionYearIdList = arrayListOf(subscriptionYearIdX1, subscriptionYearIdX2, subscriptionYearIdX3),
        subscriptionYearIdListRu = arrayListOf(subscriptionYearIdX1, subscriptionYearIdX2, subscriptionYearIdX3),
    )
}

fun SimpleActivity.launchPurchase() {
    val productIdX1 = BuildConfig.PRODUCT_ID_X1
    val productIdX2 = BuildConfig.PRODUCT_ID_X2
    val productIdX3 = BuildConfig.PRODUCT_ID_X3
    val subscriptionIdX1 = BuildConfig.SUBSCRIPTION_ID_X1
    val subscriptionIdX2 = BuildConfig.SUBSCRIPTION_ID_X2
    val subscriptionIdX3 = BuildConfig.SUBSCRIPTION_ID_X3
    val subscriptionYearIdX1 = BuildConfig.SUBSCRIPTION_YEAR_ID_X1
    val subscriptionYearIdX2 = BuildConfig.SUBSCRIPTION_YEAR_ID_X2
    val subscriptionYearIdX3 = BuildConfig.SUBSCRIPTION_YEAR_ID_X3

    startPurchaseActivity(
        R.string.app_name_g,
        productIdList = arrayListOf(productIdX1, productIdX2, productIdX3),
        productIdListRu = arrayListOf(productIdX1, productIdX2, productIdX3),
        subscriptionIdList = arrayListOf(subscriptionIdX1, subscriptionIdX2, subscriptionIdX3),
        subscriptionIdListRu = arrayListOf(subscriptionIdX1, subscriptionIdX2, subscriptionIdX3),
        subscriptionYearIdList = arrayListOf(subscriptionYearIdX1, subscriptionYearIdX2, subscriptionYearIdX3),
        subscriptionYearIdListRu = arrayListOf(subscriptionYearIdX1, subscriptionYearIdX2, subscriptionYearIdX3),
    )
}

fun Activity.newAppRecommendation() {
    if (resources.getBoolean(com.goodwy.commons.R.bool.is_foss)) {
        if (!isNewApp()) {
            if ((0..config.newAppRecommendationDialogCount).random() == 2) {
                val packageName = "radnelac.ywdoog.ved".reversed()
                NewAppDialog(
                    activity = this,
                    packageName = packageName,
                    title = getString(com.goodwy.strings.R.string.notification_of_new_application),
                    text = "AlRight Calendar",
                    drawable = AppCompatResources.getDrawable(this, com.goodwy.commons.R.drawable.ic_calendar_app_new),
                    showSubtitle = true
                ) {
                }
            }
        }
    }
}
