package com.bukhmastov.cdoitmo.fragment.settings;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;

import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class SettingsProtocolFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsGeneralFragment";
    public static final List<Preference> preferences;
    static {
        preferences = new LinkedList<>();
        preferences.add(new PreferenceList("pref_protocol_changes_weeks", "1", R.string.period_picker, R.array.pref_protocol_changes_weeks_titles, R.array.pref_protocol_changes_weeks_values, true));
        preferences.add(new PreferenceList("pref_protocol_changes_mode", "advanced", R.string.pref_mode_title, R.array.pref_protocol_changes_mode_titles, R.array.pref_protocol_changes_mode_values, true));
        preferences.add(new PreferenceSwitch("pref_protocol_changes_track", true, R.string.pref_protocol_changes_track_title, null, null));
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
