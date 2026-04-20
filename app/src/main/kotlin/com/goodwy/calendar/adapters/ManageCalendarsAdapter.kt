package com.goodwy.calendar.adapters

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.goodwy.calendar.R
import com.goodwy.calendar.activities.SimpleActivity
import com.goodwy.calendar.databinding.ItemCalendarBinding
import com.goodwy.calendar.extensions.eventsHelper
import com.goodwy.calendar.helpers.LOCAL_CALENDAR_ID
import com.goodwy.calendar.interfaces.DeleteCalendarsListener
import com.goodwy.calendar.models.CalendarEntity
import com.goodwy.commons.adapters.MyRecyclerViewAdapter
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.dialogs.RadioGroupDialog
import com.goodwy.commons.extensions.getPopupMenuTheme
import com.goodwy.commons.extensions.getProperBackgroundColor
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.setFillWithStroke
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.models.RadioItem
import com.goodwy.commons.views.MyRecyclerView

class ManageCalendarsAdapter(
    activity: SimpleActivity,
    val calendars: ArrayList<CalendarEntity>,
    val listener: DeleteCalendarsListener?,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    private val MOVE_EVENTS = 0
    private val DELETE_EVENTS = 1

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_calendar

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editCalendar()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = calendars.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = calendars.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = calendars.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(
            view = ItemCalendarBinding.inflate(activity.layoutInflater, parent, false).root
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val calendar = calendars[position]
        holder.bindView(calendar, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, calendar)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = calendars.size

    private fun getItemWithKey(key: Int): CalendarEntity? =
        calendars.firstOrNull { it.id?.toInt() == key }

    private fun getSelectedItems() =
        calendars.filter { selectedKeys.contains(it.id?.toInt()) } as ArrayList<CalendarEntity>

    private fun setupView(view: View, calendar: CalendarEntity) {
        ItemCalendarBinding.bind(view).apply {
            eventItemFrame.isSelected = selectedKeys.contains(calendar.id?.toInt())
            calendarTitle.text = calendar.getDisplayTitle()
            calendarColor.setFillWithStroke(calendar.color, activity.getProperBackgroundColor())
            calendarTitle.setTextColor(textColor)

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, calendar)
            }
        }
    }

    private fun showPopupMenu(view: View, calendar: CalendarEntity) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val calendarId = calendar.id!!.toInt()
                when (item.itemId) {
                    R.id.cab_edit -> {
                        executeItemMenuOperation(calendarId) {
                            itemClick(calendar)
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(calendarId) {
                            askConfirmDelete()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(calendarId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(calendarId)
        callback()
    }

    private fun editCalendar() {
        itemClick.invoke(getSelectedItems().first())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val calendarIds = calendars.filter { selectedKeys.contains(it.id?.toInt()) }
            .map { it.id } as ArrayList<Long>

        activity.eventsHelper.doCalendarsContainEventsOrTasks(calendarIds) {
            activity.runOnUiThread {
                if (it) {
                    val res = activity.resources
                    val items = ArrayList<RadioItem>().apply {
                        add(
                            RadioItem(
                                MOVE_EVENTS,
                                res.getString(R.string.move_events_into_default)
                            )
                        )
                        add(
                            RadioItem(
                                DELETE_EVENTS,
                                res.getString(R.string.remove_affected_events)
                            )
                        )
                    }
                    RadioGroupDialog(activity, items) {
                        deleteCalendars(it == DELETE_EVENTS)
                    }
                } else {
                    ConfirmationDialog(activity) {
                        deleteCalendars(true)
                    }
                }
            }
        }
    }

    private fun deleteCalendars(deleteEvents: Boolean) {
        val calendarsToDelete = getSelectedItems()

        for (key in selectedKeys) {
            val type = getItemWithKey(key) ?: continue
            if (type.id == LOCAL_CALENDAR_ID) {
                activity.toast(R.string.cannot_delete_default_type)
                calendarsToDelete.remove(type)
                toggleItemSelection(false, getItemKeyPosition(type.id!!.toInt()))
                break
            }
        }

        if (listener?.deleteCalendars(calendarsToDelete, deleteEvents) == true) {
            val positions = getSelectedItemPositions()
            calendars.removeAll(calendarsToDelete)
            removeSelectedItems(positions)
        }
    }
}
