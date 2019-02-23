package com.bukhmastov.cdoitmo.fragment.settings;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;

import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class SettingsSystemsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsSystemsFragment";
    public static final List<Preference> preferences;
    static {
        preferences = new LinkedList<>();
        preferences.add(new PreferenceSwitch("pref_allow_send_reports", true, R.string.pref_allow_send_reports, R.string.pref_allow_send_reports_summary, null, null));
        preferences.add(new PreferenceSwitch("pref_allow_collect_analytics", true, R.string.pref_allow_collect_analytics, R.string.pref_allow_collect_analytics_summary, null, null));
    }

    @Override
    protected List<Preference> getPreferences() {
        return preferences;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @Override
    protected Fragment getSelf() {
        return this;
    }
}
