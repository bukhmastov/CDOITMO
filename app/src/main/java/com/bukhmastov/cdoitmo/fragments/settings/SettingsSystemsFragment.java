package com.bukhmastov.cdoitmo.fragments.settings;

import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceSwitch;

import java.util.ArrayList;
import java.util.List;

public class SettingsSystemsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsSystemsFragment";
    public static ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
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
