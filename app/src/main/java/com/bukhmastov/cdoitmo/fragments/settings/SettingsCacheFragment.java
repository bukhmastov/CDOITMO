package com.bukhmastov.cdoitmo.fragments.settings;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceBasic;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceList;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceSwitch;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

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
                new ArrayList<>(Arrays.asList("pref_dynamic_refresh", "pref_static_refresh", "pref_use_university_cache", "pref_clear_cache", "pref_schedule_lessons_clear_cache", "pref_schedule_exams_clear_cache")),
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
        preferences.add(new PreferenceBasic("pref_clear_cache", null, R.string.clear_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Static.T.runThread(() -> {
                    Log.v(TAG, "pref_clear_cache clicked");
                    if (activity != null) {
                        boolean success = Storage.file.cache.clear(activity);
                        Static.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                return null;
            }
        }));
        preferences.add(new PreferenceBasic("pref_schedule_lessons_clear_cache", null, R.string.clear_schedule_lessons_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Static.T.runThread(() -> {
                    Log.v(TAG, "pref_schedule_lessons_clear_cache clicked");
                    if (activity != null) {
                        boolean success = Storage.file.cache.clear(activity, "schedule_lessons");
                        Static.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                return null;
            }
        }));
        preferences.add(new PreferenceBasic("pref_schedule_exams_clear_cache", null, R.string.clear_schedule_exams_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Static.T.runThread(() -> {
                    Log.v(TAG, "pref_schedule_exams_clear_cache clicked");
                    if (activity != null) {
                        boolean success = Storage.file.cache.clear(activity, "schedule_exams");
                        Static.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
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
