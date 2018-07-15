package com.bukhmastov.cdoitmo.provider;

import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;

public class StorageProvider {

    // future: replace with DI factory
    private static StorageProvider instance = new StorageProvider();
    public static StorageProvider instance() {
        return instance;
    }

    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();

    public Storage getStorage() {
        return storage;
    }

    public StoragePref getStoragePref() {
        return storagePref;
    }
}
