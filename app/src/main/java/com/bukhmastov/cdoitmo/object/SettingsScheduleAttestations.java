package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.impl.SettingsSchedule;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.preference.Preference;

public interface SettingsScheduleAttestations {

    // future: replace with DI factory
    SettingsScheduleAttestations instance = new SettingsScheduleAttestationsImpl();
    static SettingsScheduleAttestations instance() {
        return instance;
    }

    void show(ConnectedActivity activity, Preference preference, SettingsSchedule.Callback callback);
}
