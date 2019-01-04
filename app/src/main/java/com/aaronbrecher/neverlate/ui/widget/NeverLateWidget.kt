package com.aaronbrecher.neverlate.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.RemoteViews

import com.aaronbrecher.neverlate.AppExecutors
import com.aaronbrecher.neverlate.Constants
import com.aaronbrecher.neverlate.NeverLateApp
import com.aaronbrecher.neverlate.R
import com.aaronbrecher.neverlate.utils.DirectionsUtils
import com.aaronbrecher.neverlate.utils.GeofenceUtils
import com.aaronbrecher.neverlate.database.EventsRepository
import com.aaronbrecher.neverlate.models.Event
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity
import com.aaronbrecher.neverlate.ui.activities.MainActivity

import javax.inject.Inject

/**
 * Implementation of App Widget functionality.
 */
class NeverLateWidget : AppWidgetProvider() {

    @Inject
    lateinit var mEventsRepository: EventsRepository
    @Inject
    lateinit var sAppExecutors: AppExecutors

    private var mEvent: Event? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        NeverLateApp.app.appComponent.inject(this)
        // There may be multiple widgets active, so update all of them
        sAppExecutors.diskIO().execute {
            val events = mEventsRepository.queryAllCurrentEventsSync()
            if (events != null && events.size > 0) {
                mEvent = events[0]
            }
            sAppExecutors.mainThread().execute {
                for (widgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, mEvent)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int, event: Event?) {

            val remoteViews = createRemoteViews(context, event)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        private fun createRemoteViews(context: Context, event: Event?): RemoteViews {
            val intent: Intent
            val remoteViews = RemoteViews(context.packageName, R.layout.never_late_widget)
            if (event == null) {
                //show no event image
                intent = Intent(context, MainActivity::class.java)
            } else {
                intent = Intent(context, EventDetailActivity::class.java)
                intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, Event.convertEventToJson(event))
                val timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(event.drivingTime!!,
                        GeofenceUtils.determineRelevantTime(event.startTime, event.endTime))
                remoteViews.setTextViewText(R.id.widget_leave_time, context.getString(R.string.leave_at_time, timeToLeave))
                remoteViews.setTextViewText(R.id.widget_event_title, event.title)
                remoteViews.setTextViewText(R.id.widget_event_location, event.location)
                remoteViews.setTextViewText(R.id.widget_event_distance,
                        DirectionsUtils.getHumanReadableDistance(context, event.distance ?: -1, PreferenceManager.getDefaultSharedPreferences(context)))
                remoteViews.setTextViewText(R.id.widget_event_time_to, DirectionsUtils.readableTravelTime(event.drivingTime!!))
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            remoteViews.setPendingIntentTemplate(R.id.widget_container, pendingIntent)
            return remoteViews
        }
    }
}

