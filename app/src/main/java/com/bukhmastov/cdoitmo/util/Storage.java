package com.bukhmastov.cdoitmo.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public interface Storage {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CACHE, PERMANENT})
    @interface Mode {}
    String CACHE = "cache";
    String PERMANENT = "permanent";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({USER, GLOBAL})
    @interface Type {}
    String USER = "user";
    String GLOBAL = "general";

    boolean put(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path, String data);

    String get(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path);

    String get(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path, String def);

    boolean exists(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path);

    boolean delete(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path);

    boolean clear(@NonNull Context context, @Mode @Nullable String mode);

    boolean clear(@NonNull Context context, @Mode @Nullable String mode, @Type @NonNull String type);

    boolean clear(@NonNull Context context, @Mode @Nullable String mode, @Type @NonNull String type, @NonNull String path);

    ArrayList<String> list(@NonNull Context context, @Mode String mode, @Type @NonNull String type, @NonNull String path);

    long getDirSize(@NonNull Context context, @Mode @NonNull String mode, @Type @NonNull String type, @NonNull String path);

    void cacheReset();
}
