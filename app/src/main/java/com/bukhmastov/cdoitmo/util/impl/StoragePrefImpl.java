package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;

import java.util.Map;
import java.util.regex.Pattern;

public class StoragePrefImpl implements StoragePref {

    private static final String TAG = "StoragePref";

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, String value) {
        if (context == null) {
            Log.w(TAG, "put | key=", key, " | context is null");
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, int value) {
        if (context == null) {
            Log.w(TAG, "put | key=", key, " | context is null");
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    @Override
    public synchronized void put(@NonNull Context context, @NonNull String key, boolean value) {
        if (context == null) {
            Log.w(TAG, "put | key=", key, " | context is null");
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
            Log.w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, def);
    }

    @Override
    public int get(@NonNull Context context, @NonNull String key, int def) {
        if (context == null) {
            Log.w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, def);
    }

    @Override
    public boolean get(@NonNull Context context, @NonNull String key, boolean def) {
        if (context == null) {
            Log.w(TAG, "get | key=", key, " | context is null");
            return def;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, def);
    }

    @Override
    public boolean exists(@NonNull Context context, @NonNull String key) {
        if (context == null) {
            Log.w(TAG, "exists | key=", key, " | context is null");
            return false;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).contains(key);
    }

    @Override
    public synchronized void delete(@NonNull Context context, @NonNull String key) {
        if (context == null) {
            Log.w(TAG, "delete | key=", key, " | context is null");
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
            Log.w(TAG, "clear | context is null");
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
}
