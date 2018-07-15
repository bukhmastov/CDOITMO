package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.Log;

public class SettingsScheduleAttestations extends SettingsSchedule {

    private static final String TAG = "SettingsSA";

    public SettingsScheduleAttestations(ConnectedActivity activity, Preference preference, Callback callback) {
        super(activity, preference, callback);
    }

    @Override
    protected void search(final String q) {
        log.v(TAG, "search | query=" + q);
        search(q, (context, query, handler) -> {
            log.v(TAG, "search.onSearch | query=" + query);
            new ScheduleAttestations(handler).search(context, query);
        });
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_attestations_search_view_hint);
    }
}
