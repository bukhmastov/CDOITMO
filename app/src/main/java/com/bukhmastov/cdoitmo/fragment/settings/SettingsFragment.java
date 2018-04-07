package com.bukhmastov.cdoitmo.fragment.settings;

import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceHeader;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends SettingsTemplateHeadersFragment {

    private static final String TAG = "SettingsFragment";
    public static final List<PreferenceHeader> preferenceHeaders;
    static {
        preferenceHeaders = new ArrayList<>();
        preferenceHeaders.add(new PreferenceHeader(R.string.general_settings, R.drawable.ic_settings_applications, SettingsGeneralFragment.class));
        // TODO uncomment, when accounts links will be ready
        //preferenceHeaders.add(new PreferenceHeader(R.string.linked_accounts, R.drawable.ic_account_box, LinkedAccountsFragment.class));
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
            for (Preference preference : SettingsScheduleAttestationsFragment.preferences) applyDefaultValues(context, preference);
            for (Preference preference : SettingsSystemsFragment.preferences) applyDefaultValues(context, preference);
            Storage.pref.put(context, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
        }
    }
    private static void applyDefaultValues(final Context context, final Preference preference) {
        if (preference.defaultValue != null && !Storage.pref.exists(context, preference.key)) {
            if (preference.defaultValue instanceof String) {
                Storage.pref.put(context, preference.key, (String) preference.defaultValue);
            } else if (preference.defaultValue instanceof Integer) {
                Storage.pref.put(context, preference.key, (Integer) preference.defaultValue);
            } else if (preference.defaultValue instanceof Boolean) {
                Storage.pref.put(context, preference.key, (Boolean) preference.defaultValue);
            }
        }
    }
}
