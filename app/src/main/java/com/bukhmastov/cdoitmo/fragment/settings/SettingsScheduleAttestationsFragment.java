package com.bukhmastov.cdoitmo.fragment.settings;

import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceBasic;
import com.bukhmastov.cdoitmo.object.preference.PreferenceList;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.util.Storage;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsScheduleAttestationsFragment extends SettingsTemplatePreferencesFragment {

    private static final String TAG = "SettingsSAFragment";
    public static final ArrayList<Preference> preferences;
    static {
        preferences = new ArrayList<>();
        preferences.add(new PreferenceBasic("pref_schedule_attestations_default", "{\"query\":\"auto\",\"title\":\"\"}", R.string.default_schedule, true, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final InjectProvider injectProvider, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                injectProvider.getSettingsScheduleAttestations().show(activity, preference, value -> injectProvider.getThread().run(() -> {
                    injectProvider.getStoragePref().put(activity, "pref_schedule_attestations_default", value);
                    callback.onSetSummary(activity, value);
                }));
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
        preferences.add(new PreferenceList("pref_schedule_attestations_term", "0", R.string.term_picker, R.array.pref_schedule_attestations_term_titles, R.array.pref_schedule_attestations_term_values, true));
        preferences.add(new PreferenceSwitch("pref_schedule_attestations_use_cache", false, R.string.cache_schedule, null, null));
        preferences.add(new PreferenceBasic("pref_schedule_attestations_clear_cache", null, R.string.clear_schedule_cache, false, new PreferenceBasic.Callback() {
            @Override
            public void onPreferenceClicked(final ConnectedActivity activity, final Preference preference, final InjectProvider injectProvider, final PreferenceBasic.OnPreferenceClickedCallback callback) {
                injectProvider.getThread().run(() -> {
                    if (activity != null) {
                        boolean success = injectProvider.getStorage().clear(activity, Storage.CACHE, Storage.GLOBAL, "schedule_attestations");
                        injectProvider.getNotificationMessage().snackBar(activity, activity.getString(success ? R.string.cache_cleared : R.string.something_went_wrong));
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
