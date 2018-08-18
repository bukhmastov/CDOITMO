package com.bukhmastov.cdoitmo.util;

import android.content.Context;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;

import android.support.annotation.NonNull;

import java.util.regex.Pattern;

public interface StoragePref {

    void put(@NonNull Context context, @NonNull String key, String value);

    void put(@NonNull Context context, @NonNull String key, int value);

    void put(@NonNull Context context, @NonNull String key, boolean value);

    String get(@NonNull Context context, @NonNull String key);

    String get(@NonNull Context context, @NonNull String key, String def);

    int get(@NonNull Context context, @NonNull String key, int def);

    boolean get(@NonNull Context context, @NonNull String key, boolean def);

    boolean exists(@NonNull Context context, @NonNull String key);

    void delete(@NonNull Context context, @NonNull String key);

    void clear(@NonNull Context context);

    void clearExceptPref(@NonNull Context context);

    void clear(@NonNull Context context, @NonNull Pattern pattern);

    void applyDebug(@NonNull Context context);

    void resetIfNeeded(@NonNull ConnectedActivity activity);

    void reset(@NonNull ConnectedActivity activity);
}
