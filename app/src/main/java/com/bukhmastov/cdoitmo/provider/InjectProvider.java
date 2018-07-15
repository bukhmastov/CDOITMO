package com.bukhmastov.cdoitmo.provider;

import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

public class InjectProvider {

    // future: replace with DI factory
    private static InjectProvider instance = new InjectProvider();
    public static InjectProvider instance() {
        return instance;
    }

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();

    public Log getLog() {
        return log;
    }

    public Thread getThread() {
        return thread;
    }

    public Storage getStorage() {
        return storage;
    }

    public StoragePref getStoragePref() {
        return storagePref;
    }
}
