package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.impl.DeIfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.DeIfmo;

import java.util.Map;

public abstract class DeIfmoRestClient extends DeIfmo {

    // future: replace with DI factory
    private static DeIfmoRestClient instance = null;
    public static DeIfmoRestClient instance() {
        if (instance == null) {
            instance = new DeIfmoRestClientImpl();
        }
        return instance;
    }

    abstract public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);

    abstract public void get(@NonNull Context context, @NonNull @Client.Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);
}
