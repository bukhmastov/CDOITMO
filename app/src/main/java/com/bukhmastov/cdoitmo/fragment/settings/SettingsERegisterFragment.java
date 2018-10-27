package com.bukhmastov.cdoitmo.fragment.settings;

import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;

import java.util.ArrayList;
import java.util.List;

public class SettingsERegisterFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsERegisterFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceList("pref_e_journal_term", "0", R.string.term_picker, R.array.pref_e_journal_term_titles, R.array.pref_e_journal_term_values, true));
        preferences.add(new PreferenceList("pref_eregister_mode", "advanced", R.string.pref_mode_title, R.array.pref_eregister_mode_titles, R.array.pref_eregister_mode_values, true));
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
