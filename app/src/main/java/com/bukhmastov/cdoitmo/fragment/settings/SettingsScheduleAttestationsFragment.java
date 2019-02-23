package com.bukhmastov.cdoitmo.fragment.settings;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.model.entity.SettingsQuery;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.Fragment;

public class SettingsScheduleAttestationsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsSAFragment";
    public static final List<Preference> preferences;
    static {
        preferences = new LinkedList<>();
        preferences.add(new PreferenceBasic("pref_schedule_attestations_default", "{\"query\":\"auto\",\"title\":\"\"}", R.string.default_schedule, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                injectProvider.getSettingsScheduleAttestations().show(activity, preference, value -> {
                    injectProvider.getStoragePref().put(activity, "pref_schedule_attestations_default", value);
                    callback.onSetSummary(activity, value);
                });
            }
            @Override
            public String onGetSummary(Context context, String value) {
                try {
                    SettingsQuery settingsQuery = new SettingsQuery().fromJsonString(value);
                    switch (settingsQuery.getQuery()) {
                        case "personal": return context.getString(R.string.personal_schedule);
                        case "auto": return context.getString(R.string.current_group);
                        default: return settingsQuery.getTitle();
                    }
                } catch (Exception e) {
                    return null;
                }
            }
        }));
        preferences.add(new PreferenceList("pref_schedule_attestations_term", "0", R.string.term_picker, R.array.pref_schedule_attestations_term_titles, R.array.pref_schedule_attestations_term_values, true));
        preferences.add(new PreferenceSwitch("pref_schedule_attestations_use_cache", false, R.string.cache_schedule, null, null));
        preferences.add(new PreferenceBasic("pref_schedule_attestations_clear_cache", null, R.string.clear_schedule_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(ConnectedActivity activity, Preference preference, InjectProvider injectProvider, PreferenceBasic.OnPreferenceClickedCallback callback) {
                injectProvider.getThread().standalone(() -> {
                    if (activity != null) {
                        boolean success = injectProvider.getStorage().clear(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations");
                        if (success) {
                            injectProvider.getNotificationMessage().snackBar(activity, activity.getString(R.string.cache_cleared));
                        }
                    }
                });
            }
            @Override
            public String onGetSummary(Context context, String value) {
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
