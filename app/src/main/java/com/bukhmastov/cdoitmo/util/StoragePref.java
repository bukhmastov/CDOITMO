package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.impl.StoragePrefImpl;

import java.util.regex.Pattern;

public interface StoragePref {

    // future: replace with DI factory
    StoragePref instance = new StoragePrefImpl();
    static StoragePref instance() {
        return instance;
    }

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
}
