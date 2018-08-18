package com.bukhmastov.cdoitmo.fragment.settings;

import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceImitation;

import java.util.ArrayList;
import java.util.List;

public class SettingsUncategorized {

    public static final List<Preference> preferences;

    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceImitation("pref_tab_refresh", "0", true));
        preferences.add(new PreferenceImitation("pref_schedule_refresh", "168", true));
        preferences.add(new PreferenceImitation("pref_university_tab", -1, true));
        preferences.add(new PreferenceImitation("pref_allow_collect_logs", false, true));
        preferences.add(new PreferenceImitation("pref_university_buildings_dormitory", true, true));
        preferences.add(new PreferenceImitation("pref_university_buildings_campus", true, true));
        preferences.add(new PreferenceImitation("pref_uuid", "", false));
        preferences.add(new PreferenceImitation("last_version", 0, false));
        preferences.add(new PreferenceImitation("pref_first_launch", true, false));
        preferences.add(new PreferenceImitation("pref_default_values_applied", false, false));
    }
}

