package com.bukhmastov.cdoitmo.object;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.impl.SettingsSchedule;
import com.bukhmastov.cdoitmo.object.preference.Preference;

public interface SettingsScheduleExams {

    void show(ConnectedActivity activity, Preference preference, SettingsSchedule.Callback callback);
}
