package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.impl.IfmoClientImpl;
import com.bukhmastov.cdoitmo.network.model.Ifmo;

import java.util.Map;

public abstract class IfmoClient extends Ifmo {

    // future: replace with DI factory
    private static IfmoClient instance = null;
    public static IfmoClient instance() {
        if (instance == null) {
            instance = new IfmoClientImpl();
        }
        return instance;
    }

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);
}
