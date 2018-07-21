package com.bukhmastov.cdoitmo.fragment.settings;

import android.content.Context;
import android.os.Build;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceHeader;
import com.bukhmastov.cdoitmo.util.StoragePref;

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

    public static void applyDefaultValues(final Context context, final StoragePref storagePref) {
        if (!storagePref.get(context, "pref_default_values_applied", false)) {
            storagePref.put(context, "pref_default_values_applied", true);
            for (Preference preference : SettingsGeneralFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsCacheFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsNotificationsFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsERegisterFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsProtocolFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsScheduleLessonsFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsScheduleExamsFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsScheduleAttestationsFragment.preferences) preference.applyDefaultValue(context);
            for (Preference preference : SettingsSystemsFragment.preferences) preference.applyDefaultValue(context);
            storagePref.put(context, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
        }
    }
}
