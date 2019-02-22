package com.bukhmastov.cdoitmo.object.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.object.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import javax.inject.Inject;

public class SettingsScheduleAttestationsImpl extends SettingsSchedule<SAttestations> implements SettingsScheduleAttestations {

    private static final String TAG = "SettingsSA";

    @Inject
    ScheduleAttestations scheduleAttestations;

    public SettingsScheduleAttestationsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void show(ConnectedActivity activity, Preference preference, Consumer<String> callback) {
        super.show(activity, preference, callback);
    }

    @Override
    protected void search(String query) {
        log.v(TAG, "search | query=" + query);
        scheduleAttestations.search(query, this);
    }

    @Override
    public void onSuccess(SAttestations schedule, boolean fromCache) {
        try {
            log.v(TAG, "search | onSuccess | schedule=", (schedule == null ? "null" : "notnull"));
            toggleSearchState("action");
            if (schedule == null || StringUtils.isBlank(schedule.getType())) {
                notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found));
                return;
            }
            switch (schedule.getType()) {
                case "group": {
                    if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                        return;
                    }
                    query = schedule.getQuery();
                    title = schedule.getTitle();
                    log.v(TAG, "search | onSuccess | done | query=", query, " | title=", title);
                    toggleSearchState("selected");
                    break;
                }
                default: {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    break;
                }
            }
        } catch (Throwable throwable) {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        }
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_attestations_search_view_hint);
    }
}
