package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.impl.SettingsSchedule;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.object.preference.Preference;

public interface SettingsScheduleLessons {

    // future: replace with DI factory
    SettingsScheduleLessons instance = new SettingsScheduleLessonsImpl();
    static SettingsScheduleLessons instance() {
        return instance;
    }

    void show(ConnectedActivity activity, Preference preference, SettingsSchedule.Callback callback);
}
