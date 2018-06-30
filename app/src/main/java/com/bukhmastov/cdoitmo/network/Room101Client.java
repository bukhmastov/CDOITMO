package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.impl.Room101ClientImpl;
import com.bukhmastov.cdoitmo.network.model.Room101;

import java.util.Map;

public abstract class Room101Client extends Room101 {

    // future: replace with DI factory
    private static Room101Client instance = null;
    public static Room101Client instance() {
        if (instance == null) {
            instance = new Room101ClientImpl();
        }
        return instance;
    }

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void post(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    public abstract void post(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    @Override
    public abstract boolean isAuthorized(@NonNull Context context);
}
