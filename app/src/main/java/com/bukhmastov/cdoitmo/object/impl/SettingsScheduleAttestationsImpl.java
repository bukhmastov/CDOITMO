package com.bukhmastov.cdoitmo.object.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;

public class SettingsScheduleAttestationsImpl extends SettingsSchedule implements SettingsScheduleAttestations {

    private static final String TAG = "SettingsSA";

    //@Inject
    private ScheduleAttestations scheduleAttestations = ScheduleAttestations.instance();

    @Override
    public void show(ConnectedActivity activity, Preference preference, Callback callback) {
        super.show(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        log.v(TAG, "search | query=" + q);
        search(q, (context, query, handler) -> {
            log.v(TAG, "search.onSearch | query=" + query);
            scheduleAttestations.init(handler).search(context, query);
        });
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_attestations_search_view_hint);
    }
}
