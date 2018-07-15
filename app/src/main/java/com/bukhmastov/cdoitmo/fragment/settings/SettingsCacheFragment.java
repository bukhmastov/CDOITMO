package com.bukhmastov.cdoitmo.fragment.settings;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.dialog.CacheClearDialog;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.util.StorageProvider;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsCacheFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsCacheFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceSwitch(
                "pref_use_cache",
                true,
                R.string.pref_use_cache,
                new ArrayList<>(Arrays.asList("pref_dynamic_refresh", "pref_static_refresh", "pref_use_university_cache", "pref_clear_cache", "pref_schedule_lessons_clear_cache", "pref_schedule_exams_clear_cache", "pref_schedule_attestations_clear_cache", "pref_university_clear_cache")),
                (activity, preference, value, callback) -> {
                    if (!value) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.pref_use_cache)
                                .setMessage(R.string.pref_use_cache_message)
                                .setPositiveButton(R.string.proceed, (dialog, which) -> callback.onDecisionMade(activity, preference, true))
                                .setNegativeButton(R.string.cancel, (dialog, which) -> callback.onDecisionMade(activity, preference, false))
                                .setOnCancelListener(dialog -> callback.onDecisionMade(activity, preference, false))
                                .create().show();
                    } else {
                        callback.onDecisionMade(activity, preference, true);
                    }
                }
        ));
        preferences.add(new PreferenceList("pref_dynamic_refresh", "0", R.string.pref_dynamic_refresh, R.array.pref_refresh_titles, R.array.pref_refresh_values, true));
        preferences.add(new PreferenceList("pref_static_refresh", "168", R.string.pref_static_refresh, R.array.pref_refresh_titles, R.array.pref_refresh_values, true));
        preferences.add(new PreferenceSwitch("pref_use_university_cache", false, R.string.pref_use_university_cache, R.string.pref_use_university_cache_summary, null, null));
        preferences.add(new PreferenceBasic("pref_clear_cache", null, R.string.cache_clear, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final StorageProvider storageProvider, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Thread.run(() -> new CacheClearDialog(activity).show());
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
