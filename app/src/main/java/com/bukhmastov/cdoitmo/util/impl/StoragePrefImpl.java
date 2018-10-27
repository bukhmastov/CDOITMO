package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsCacheFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsERegisterFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsGeneralFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsNotificationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsProtocolFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsSystemsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsUncategorized;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.Lazy;

public class StoragePrefImpl implements StoragePref {

    private static final String TAG = "StoragePref";
    private Collection<Preference> preferences = new ArrayList<>();

    @Inject
    Lazy<Log> log;
    @Inject
    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;
    @Inject
    Lazy<FirebaseCrashlyticsProvider> firebaseCrashlyticsProvider;

    public StoragePrefImpl() {
        AppComponentProvider.getComponent().inject(this);
        preferences.clear();
        preferences.addAll(SettingsGeneralFragment.preferences);
        preferences.addAll(SettingsCacheFragment.preferences);
        preferences.addAll(SettingsNotificationsFragment.preferences);
        preferences.addAll(SettingsERegisterFragment.preferences);
        preferences.addAll(SettingsProtocolFragment.preferences);
        preferences.addAll(SettingsScheduleLessonsFragment.preferences);
        preferences.addAll(SettingsScheduleExamsFragment.preferences);
        preferences.addAll(SettingsScheduleAttestationsFragment.preferences);
        preferences.addAll(SettingsSystemsFragment.preferences);
        preferences.addAll(SettingsUncategorized.preferences);
    }

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, String value) {
        if (context == null) {
            log.get().w(TAG, "put | key=", key, " | context is null");
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, int value) {
        if (context == null) {
            log.get().w(TAG, "put | key=", key, " | context is null");
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, boolean value) {
        if (context == null) {
            log.get().w(TAG, "put | key=", key, " | context is null");
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    @Override
    public String get(@NonNull Context context, @NonNull String key) {
        return get(context, key, "");
    }

    @Override
    public String get(@NonNull Context context, @NonNull String key, String def) {
        if (context == null) {
            log.get().w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
    }

    @Override
    public int get(@NonNull Context context, @NonNull String key, int def) {
        if (context == null) {
            log.get().w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, def);
    }

    @Override
    public boolean get(@NonNull Context context, @NonNull String key, boolean def) {
        if (context == null) {
            log.get().w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, def);
    }

    @Override
    public boolean exists(@NonNull Context context, @NonNull String key) {
        if (context == null) {
            log.get().w(TAG, "exists | key=", key, " | context is null");
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).contains(key);
    }

    @Override
    public synchronized void delete(@NonNull Context context, @NonNull String key) {
        if (context == null) {
            log.get().w(TAG, "delete | key=", key, " | context is null");
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.contains(key)) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

    @Override
    public void clear(@NonNull Context context) {
        clear(context, Pattern.compile(".*"));
    }

    @Override
    public void clearExceptPref(@NonNull Context context) {
        clear(context, Pattern.compile("^(?!pref_).*$"));
    }

    @Override
    public synchronized void clear(@NonNull Context context, @NonNull Pattern pattern) {
        if (context == null) {
            log.get().w(TAG, "clear | context is null");
            return;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> list = sharedPreferences.getAll();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, ?> entry : list.entrySet()) {
            if (pattern.matcher(entry.getKey()).find()) {
                String key = entry.getKey();
                editor.remove(key);
            }
        }
        editor.apply();
    }

    @Override
    public void applyDebug(Context context) {
        if (App.DEBUG) {
            put(context, "pref_allow_collect_analytics", false);
            put(context, "pref_allow_send_reports", false);
            put(context, "pref_allow_collect_logs", true);
            firebaseAnalyticsProvider.get().setEnabled(context, false);
            firebaseCrashlyticsProvider.get().setEnabled(context, false);
            log.get().i(TAG, "Currently running with debug mode, preferences has been reset to debug mode");
        }
    }

    @Override
    public void resetIfNeeded(ConnectedActivity activity) {
        if (!get(activity, "pref_default_values_applied", false)) {
            put(activity, "pref_default_values_applied", true);
            reset(activity, false);
        }
    }

    @Override
    public void reset(ConnectedActivity activity) {
        reset(activity, true);
    }

    private void reset(ConnectedActivity activity, boolean fire) {
        for (Preference preference : preferences) {
            Object previousValue = fire ? preference.getValue(activity) : null;
            preference.applyDefaultValue(activity);
            if (fire) {
                Object currentValue = preference.getValue(activity);
                if (!Objects.equals(previousValue, currentValue)) {
                    preference.onPreferenceChanged(activity);
                }
            }
        }
        put(activity, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
    }
}
