package com.bukhmastov.cdoitmo.object.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;

public class SettingsScheduleLessonsImpl extends SettingsSchedule implements SettingsScheduleLessons {

    private static final String TAG = "SettingsSL";

    //@Inject
    private ScheduleLessons scheduleLessons = ScheduleLessons.instance();

    @Override
    public void show(ConnectedActivity activity, Preference preference, Callback callback) {
        super.show(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        log.v(TAG, "search | query=" + q);
        search(q, (context, query, handler) -> {
            log.v(TAG, "search.onSearch | query=" + query);
            scheduleLessons.init(handler).search(context, query);
        });
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_lessons_search_view_hint);
    }
}
