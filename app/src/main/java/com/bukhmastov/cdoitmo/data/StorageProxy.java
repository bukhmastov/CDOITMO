package com.bukhmastov.cdoitmo.data;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.Storage;

public interface StorageProxy {
    @NonNull String get(Storage.StorageType type, String path);
    boolean put(Storage.StorageType type, String path, String data);
    boolean delete(Storage.StorageType type, String path);
}
