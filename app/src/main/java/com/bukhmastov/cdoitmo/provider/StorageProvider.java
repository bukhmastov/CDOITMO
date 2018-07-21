package com.bukhmastov.cdoitmo.provider;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;

import javax.inject.Inject;

import dagger.Lazy;

public class StorageProvider {

    @Inject
    Lazy<Storage> storage;
    @Inject
    Lazy<StoragePref> storagePref;

    public StorageProvider() {
        AppComponentProvider.getComponent().inject(this);
    }

    public Storage getStorage() {
        return storage.get();
    }

    public StoragePref getStoragePref() {
        return storagePref.get();
    }
}
