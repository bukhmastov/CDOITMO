package com.bukhmastov.cdoitmo.util;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.impl.StorageLocalCacheImpl;

public interface StorageLocalCache {

    // future: replace with DI factory
    StorageLocalCache instance = new StorageLocalCacheImpl();
    static StorageLocalCache instance() {
        return instance;
    }

    void push(@NonNull String path, String data, double priority);

    void access(@NonNull String path);

    String get(@NonNull String path);

    void delete(@NonNull String path);

    void check();

    void reset();
}
