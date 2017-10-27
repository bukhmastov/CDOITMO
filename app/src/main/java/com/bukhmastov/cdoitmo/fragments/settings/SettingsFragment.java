package com.bukhmastov.cdoitmo.fragments.settings;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.AboutFragment;
import com.bukhmastov.cdoitmo.objects.preferences.Preference;
import com.bukhmastov.cdoitmo.objects.preferences.PreferenceHeader;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends SettingsTemplateHeadersFragment {

    private static final String TAG = "SettingsFragment";
    public static List<PreferenceHeader> preferenceHeaders;
    static {
        preferenceHeaders = new ArrayList<>();
        preferenceHeaders.add(new PreferenceHeader(R.string.general_settings, R.drawable.ic_settings_applications, SettingsGeneralFragment.class));
        //preferenceHeaders.add(new PreferenceHeader(R.string.linked_accounts, R.drawable.ic_account_box, SettingsGeneralFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.cache_and_refresh, R.drawable.ic_save, SettingsCacheFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.notifications, R.drawable.ic_notifications, SettingsNotificationsFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.extended_prefs, R.drawable.ic_tune, SettingsExtendedFragment.class));
        preferenceHeaders.add(new PreferenceHeader(R.string.about, R.drawable.ic_info_outline, AboutFragment.class));
    }

    @Override
    protected List<PreferenceHeader> getPreferenceHeaders() {
        return preferenceHeaders;
    }

    @Override
    protected String getTAG() {
        return TAG;
    }

    @Override
    protected Fragment getSelf() {
        return this;
    }

    public static void applyDefaultValues(final Context context) {
        if (!Storage.pref.get(context, "pref_default_values_applied", false)) {
            Storage.pref.put(context, "pref_default_values_applied", true);
            for (Preference preference : SettingsGeneralFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsCacheFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsNotificationsFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsERegisterFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsProtocolFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsScheduleLessonsFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsScheduleExamsFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsSystemsFragment.preferences) applyDefaultValues(context, preference);
        }
    }
    private static void applyDefaultValues(final Context context, final Preference preference) {
        if (preference.defaultValue != null) {
            if (preference.defaultValue instanceof String) {
                if ("pref_undefined".equals(Storage.pref.get(context, preference.key, "pref_undefined"))) {
                    Storage.pref.put(context, preference.key, (String) preference.defaultValue);
                }
            } else if (preference.defaultValue instanceof Integer) {
                if (Storage.pref.get(context, preference.key, Integer.MIN_VALUE) == Integer.MIN_VALUE) {
                    Storage.pref.put(context, preference.key, (Integer) preference.defaultValue);
                }
            } else if (preference.defaultValue instanceof Boolean) {
                if (Storage.pref.get(context, preference.key, null) == null) {
                    Storage.pref.put(context, preference.key, (Boolean) preference.defaultValue);
                }
            }
        }
    }
}
