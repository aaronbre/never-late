package com.aaronbrecher.neverlate.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.aaronbrecher.neverlate.AppExecutors;
import com.aaronbrecher.neverlate.Constants;
import com.aaronbrecher.neverlate.NeverLateApp;
import com.aaronbrecher.neverlate.R;
import com.aaronbrecher.neverlate.Utils.DirectionsUtils;
import com.aaronbrecher.neverlate.Utils.GeofenceUtils;
import com.aaronbrecher.neverlate.Utils.LocationUtils;
import com.aaronbrecher.neverlate.database.EventsRepository;
import com.aaronbrecher.neverlate.models.Event;
import com.aaronbrecher.neverlate.ui.activities.EventDetailActivity;
import com.aaronbrecher.neverlate.ui.activities.MainActivity;

import org.joda.time.LocalDateTime;

import java.util.List;

import javax.inject.Inject;

/**
 * Implementation of App Widget functionality.
 */
public class NeverLateWidget extends AppWidgetProvider {

    @Inject
    EventsRepository mEventsRepository;
    @Inject
    AppExecutors sAppExecutors;
    private Event mEvent;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, Event event) {

        RemoteViews remoteViews = createRemoteViews(context, event);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        NeverLateApp.getApp().getAppComponent().inject(this);
        // There may be multiple widgets active, so update all of them
        sAppExecutors.diskIO().execute(()->{
            List<Event> events = mEventsRepository.queryAllCurrentEventsSync();
            if(events != null && events.size() > 0){
                mEvent = events.get(0);
            }
            sAppExecutors.mainThread().execute(() ->{
                for (int widgetId : appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, mEvent);
                }
            });
        });
    }

    private static RemoteViews createRemoteViews(Context context, Event event) {
        Intent intent;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.never_late_widget);
        if (event == null) {
            //show no event image
            intent = new Intent(context, MainActivity.class);
        } else {
            intent = new Intent(context, EventDetailActivity.class);
            intent.putExtra(Constants.EVENT_DETAIL_INTENT_EXTRA, event);
            String timeToLeave = DirectionsUtils.getTimeToLeaveHumanReadable(event.getTimeTo(),
                    GeofenceUtils.determineRelevantTime(event.getStartTime(), event.getEndTime()));
            remoteViews.setTextViewText(R.id.widget_leave_time, "Leave at about - " + timeToLeave);
            remoteViews.setTextViewText(R.id.widget_event_title, event.getTitle());
            remoteViews.setTextViewText(R.id.widget_event_location, event.getLocation());
            remoteViews.setTextViewText(R.id.widget_event_distance,
                    DirectionsUtils.getHumanReadableDistance(event.getDistance(), PreferenceManager.getDefaultSharedPreferences(context)));
            remoteViews.setTextViewText(R.id.widget_event_time_to, DirectionsUtils.readableTravelTime(event.getTimeTo()));
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
        remoteViews.setPendingIntentTemplate(R.id.widget_container, pendingIntent);
        return remoteViews;
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

