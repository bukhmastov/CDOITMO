package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.util.Log;

public class SettingsScheduleExams extends SettingsSchedule {

    private static final String TAG = "SettingsSE";

    public SettingsScheduleExams(ConnectedActivity activity, Preference preference, Callback callback) {
        super(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        Log.v(TAG, "search | query=" + q);
        search(q, (context, query, handler) -> {
            Log.v(TAG, "search.onSearch | query=" + query);
            new ScheduleExams(handler).search(context, query);
        });
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_exams_search_view_hint);
    }
}
