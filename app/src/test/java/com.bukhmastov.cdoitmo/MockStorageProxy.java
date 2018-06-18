package com.bukhmastov.cdoitmo;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.data.StorageProxy;
import com.bukhmastov.cdoitmo.data.UserCredentials;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.HashMap;

public class MockStorageProxy implements StorageProxy {
    private HashMap<String, String> global = new HashMap<>();
    private HashMap<String, String> prefs = new HashMap<>();
    private HashMap<String, HashMap<String, String>> perUser = new HashMap<>();

    @NonNull
    @Override
    public String get(Storage.StorageType type, String path) {
        String data = getHashMap(type).get(path);
        if (data == null) return "";
        else return data;
    }

    @Override
    public boolean put(Storage.StorageType type, String path, String data) {
        getHashMap(type).put(path, data);
        return true;
    }

    @Override
    public boolean delete(Storage.StorageType type, String path) {
        return getHashMap(type).remove(path) != null;
    }

    private HashMap<String, String> getHashMap(Storage.StorageType type) {
        switch (type) {
            case PER_USER:
                String user = UserCredentials.getCurrentLogin(this);
                if (!perUser.containsKey(user)) perUser.put(user, new HashMap<>());
                return perUser.get(user);
            case GLOBAL: return global;
            default: return prefs;
        }
    }
}
