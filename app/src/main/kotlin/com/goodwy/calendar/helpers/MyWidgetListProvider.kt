package com.goodwy.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SplashActivity
import com.goodwy.calendar.extensions.config
import com.goodwy.calendar.extensions.getWidgetFontSize
import com.goodwy.calendar.extensions.launchNewEventOrTaskActivity
import com.goodwy.calendar.extensions.widgetsDB
import com.goodwy.calendar.services.WidgetService
import com.goodwy.calendar.services.WidgetServiceEmpty
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.ensureBackgroundThread
import org.joda.time.DateTime
import androidx.core.net.toUri

class MyWidgetListProvider : AppWidgetProvider() {
    private val NEW_EVENT = "new_event"
    private val LAUNCH_CAL = "launch_cal"
    private val GO_TO_TODAY = "go_to_today"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val fontSize = context.getWidgetFontSize()
        val textColor = context.config.widgetTextColor
        val secondTextColor = context.config.widgetSecondTextColor

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        ensureBackgroundThread {
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val widget = context.widgetsDB.getWidgetWithWidgetId(it)

                val headerVisibility = if (widget?.header == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                    applyColorFilter(R.id.widget_event_list_background, context.config.widgetBgColor)
                    setTextColor(R.id.widget_event_list_empty, secondTextColor)
                    setTextSize(R.id.widget_event_list_empty, fontSize)

                    setTextColor(R.id.widget_event_list_today, textColor)
                    setTextSize(R.id.widget_event_list_today, fontSize - 3f)
                    setVisibleIf(R.id.widget_name, context.config.showWidgetName)
                    setTextColor(R.id.widget_name, context.config.widgetLabelColor)

                    setViewVisibility(R.id.widget_header_include, headerVisibility)
                }

                views.setImageViewBitmap(
                    R.id.widget_event_new_event, context.resources.getColoredBitmap(
                        resourceId = com.goodwy.commons.R.drawable.ic_plus_vector,
                        newColor = textColor
                    )
                )
                setupIntent(context, views, NEW_EVENT, R.id.widget_event_new_event)
                setupIntent(context, views, LAUNCH_CAL, R.id.widget_event_list_today)

                views.setImageViewBitmap(R.id.widget_event_go_to_today, context.resources.getColoredBitmap(R.drawable.ic_today_vector, textColor))
                setupIntent(context, views, GO_TO_TODAY, R.id.widget_event_go_to_today)

                Intent(context, WidgetService::class.java).apply {
                    putExtra(EVENT_LIST_PERIOD, widget?.period)
                    data = this.toUri(Intent.URI_INTENT_SCHEME).toUri()
                    views.setRemoteAdapter(R.id.widget_event_list, this)
                }

                val startActivityIntent = context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)
                val startActivityPendingIntent =
                    PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                views.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)
                views.setEmptyView(R.id.widget_event_list, R.id.widget_event_list_empty)

                appWidgetManager.updateAppWidget(it, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_event_list)
            }
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetListProvider::class.java)

    private fun setupIntent(context: Context, views: RemoteViews, action: String, id: Int) {
        Intent(context, MyWidgetListProvider::class.java).apply {
            this.action = action
            val pendingIntent = PendingIntent.getBroadcast(context, 0, this, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NEW_EVENT -> context.launchNewEventOrTaskActivity()
            LAUNCH_CAL -> launchCalenderInDefaultView(context)
            GO_TO_TODAY -> goToToday(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        ensureBackgroundThread {
            appWidgetIds?.forEach {
                context?.widgetsDB?.deleteWidgetId(it)
            }
        }
    }

    private fun launchCalenderInDefaultView(context: Context) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            putExtra(DAY_CODE, Formatter.getDayCodeFromDateTime(DateTime()))
            putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }

    // hacky solution for reseting the events list
    private fun goToToday(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        ensureBackgroundThread {
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val widget = context.widgetsDB.getWidgetWithWidgetId(it)
                val headerVisibility = if (widget?.header == true) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                    setViewVisibility(R.id.widget_event_list_today, headerVisibility)
                    setViewVisibility(R.id.widget_event_go_to_today, headerVisibility)
                    setViewVisibility(R.id.widget_event_new_event, headerVisibility)
                }
                Intent(context, WidgetServiceEmpty::class.java).apply {
                    data = this.toUri(Intent.URI_INTENT_SCHEME).toUri()
                    views.setRemoteAdapter(R.id.widget_event_list, this)
                }

                appWidgetManager.updateAppWidget(it, views)
            }

            performUpdate(context)
        }
    }
}
