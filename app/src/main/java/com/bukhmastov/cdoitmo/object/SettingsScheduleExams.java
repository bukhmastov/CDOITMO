package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.impl.SettingsSchedule;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.preference.Preference;

public interface SettingsScheduleExams {

    // future: replace with DI factory
    SettingsScheduleExams instance = new SettingsScheduleExamsImpl();
    static SettingsScheduleExams instance() {
        return instance;
    }

    void show(ConnectedActivity activity, Preference preference, SettingsSchedule.Callback callback);
}
