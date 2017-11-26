package com.bukhmastov.cdoitmo.objects;

import android.content.Context;

import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;

public class SettingsScheduleLessons extends SettingsSchedule {

    private static final String TAG = "SettingsScheduleLessons";

    public SettingsScheduleLessons(ConnectedActivity activity, Preference preference, Callback callback) {
        super(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        Log.v(TAG, "search | query=" + q);
        search(q, new Schedule.ScheduleSearchProvider() {
            @Override
            public void onSearch(Context context, String query, Schedule.Handler handler) {
                Log.v(TAG, "search.onSearch | query=" + query);
                new ScheduleLessons(handler).search(context, query);
            }
        });
    }
}
