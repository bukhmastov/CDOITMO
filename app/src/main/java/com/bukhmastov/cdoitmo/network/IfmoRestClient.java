package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.impl.IfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.model.Ifmo;

import java.util.Map;

public abstract class IfmoRestClient extends Ifmo {

    // future: replace with DI factory
    private static IfmoRestClient instance = null;
    public static IfmoRestClient instance() {
        if (instance == null) {
            instance = new IfmoRestClientImpl();
        }
        return instance;
    }

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);
}
