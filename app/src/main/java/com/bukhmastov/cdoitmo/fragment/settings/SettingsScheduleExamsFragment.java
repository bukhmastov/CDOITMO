package com.bukhmastov.cdoitmo.fragment.settings;

import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.SettingsScheduleExams;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StorageProvider;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsScheduleExamsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsScheduleExamsFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceBasic("pref_schedule_exams_default", "{\"query\":\"auto\",\"title\":\"\"}", R.string.default_schedule, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final StorageProvider storageProvider, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                new SettingsScheduleExams(activity, preference, value -> Thread.run(() -> {
                    storageProvider.getStoragePref().put(activity, "pref_schedule_exams_default", value);
                    callback.onSetSummary(activity, value);
                })).show();
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                try {
                    final JSONObject json = new JSONObject(value);
                    switch (json.getString("query")) {
                        case "mine": return activity.getString(R.string.personal_schedule);
                        case "auto": return activity.getString(R.string.current_group);
                        default: return json.getString("title");
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        // TODO implement when isu will be ready
        // pref_schedule_exams_source | PreferenceList | [ifmo, isu]
        preferences.add(new PreferenceList("pref_schedule_exams_type", "0", R.string.default_tab, R.array.pref_schedule_exams_type_titles, R.array.pref_schedule_exams_type_values, true));
        preferences.add(new PreferenceSwitch("pref_schedule_exams_use_cache", false, R.string.cache_schedule, null, null));
        preferences.add(new PreferenceBasic("pref_schedule_exams_clear_cache", null, R.string.clear_schedule_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final StorageProvider storageProvider, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Thread.run(() -> {
                    if (activity != null) {
                        boolean success = storageProvider.getStorage().clear(activity, Storage.CACHE, Storage.GLOBAL, "schedule_exams");
                        BottomBar.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                return null;
            }
        }));
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
