package com.bukhmastov.cdoitmo.util;

import androidx.annotation.NonNull;

public interface StorageLocalCache {

    void push(@NonNull String path, String data, double priority);

    void access(@NonNull String path);

    String get(@NonNull String path);

    void delete(@NonNull String path);

    void check();

    void reset();
}
