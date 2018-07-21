package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.DeIfmo;

import java.util.Map;

public abstract class DeIfmoClient extends DeIfmo {

    public DeIfmoClient() {
        super();
    }

    abstract public void check(@NonNull Context context, @NonNull ResponseHandler responseHandler);

    abstract public void authorize(@NonNull Context context, @NonNull ResponseHandler responseHandler);

    abstract public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    abstract public void get(@NonNull Context context, @NonNull @Client.Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull  ResponseHandler responseHandler);

    abstract public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler, boolean reAuth);

    abstract public void get(@NonNull Context context, @NonNull @Client.Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler, boolean reAuth);

    abstract public void post(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    abstract public void post(@NonNull Context context, @NonNull @Client.Protocol String protocol, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    @Override
    abstract public boolean isAuthorized(@NonNull Context context);
}
