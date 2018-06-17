package com.bukhmastov.cdoitmo.fragment.settings;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsScheduleLessonsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsScheduleLessonsFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceBasic("pref_schedule_lessons_default", "{\"query\":\"auto\",\"title\":\"\"}", R.string.default_schedule, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                new SettingsScheduleLessons(activity, preference, value -> Thread.run(() -> {
                    Storage.pref.put(activity, "pref_schedule_lessons_default", value);
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
                    Log.exception(e);
                    return null;
                }
            }
        }));
        // TODO implement when isu will be ready
        // pref_schedule_lessons_source | PreferenceList | [ifmo, isu]
        preferences.add(new PreferenceList("pref_schedule_lessons_week", "-1", R.string.week_picker, R.array.pref_schedule_lessons_week_titles, R.array.pref_schedule_lessons_week_values, true));
        preferences.add(new PreferenceList("pref_schedule_lessons_view_of_reduced_lesson", "compact", R.string.pref_schedule_lessons_view_of_reduced_lesson, R.array.pref_schedule_lessons_view_of_reduced_lesson_titles, R.array.pref_schedule_lessons_view_of_reduced_lesson_values, true));
        preferences.add(new PreferenceSwitch("pref_schedule_lessons_scroll_to_day", true, R.string.pref_schedule_lessons_scroll_to_day, null, null));
        preferences.add(new PreferenceSwitch("pref_schedule_lessons_use_cache", false, R.string.cache_schedule, null, null));
        preferences.add(new PreferenceBasic("pref_schedule_lessons_clear_cache", null, R.string.clear_schedule_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                Thread.run(() -> {
                    Log.v(TAG, "pref_schedule_lessons_clear_cache clicked");
                    if (activity != null) {
                        boolean success = Storage.file.general.cache.clear(activity, "schedule_lessons");
                        BottomBar.snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
                    }
                });
            }
            @Override
            public String onGetSummary(ConnectedActivity activity, String value) {
                return null;
            }
        }));
        preferences.add(new PreferenceBasic("pref_schedule_lessons_clear_additional", null, R.string.pref_schedule_lessons_clear_additional_title, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, Preference preference, PreferenceBasic.OnPreferenceClickedCallback callback) {
                Thread.runOnUI(() -> {
                    Log.v(TAG, "pref_schedule_lessons_clear_additional clicked");
                    if (activity != null) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                                .setMessage(R.string.pref_schedule_lessons_clear_additional_warning)
                                .setIcon(R.drawable.ic_warning)
                                .setPositiveButton(R.string.proceed, (dialog, which) -> Thread.run(() -> {
                                    Log.v(TAG, "pref_schedule_lessons_clear_additional dialog accepted");
                                    boolean success = Storage.file.perm.clear(activity, "schedule_lessons");
                                    BottomBar.snackBar(activity, activity.getString(success ? R.string.changes_cleared : R.string.something_went_wrong));
                                }))
                                .setNegativeButton(R.string.cancel, null)
                                .create().show();
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
