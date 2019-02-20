package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.DeIfmo;

import java.util.Map;

public abstract class DeIfmoClient extends DeIfmo {

    public DeIfmoClient() {
        super();
    }

    public abstract void authorize(@NonNull Context context, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Client.Protocol String protocol,
                             @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull ResponseHandler responseHandler, boolean reAuth);

    public abstract void get(@NonNull Context context, @NonNull @Client.Protocol String protocol,
                             @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull ResponseHandler responseHandler, boolean reAuth);

    public abstract void post(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> params,
                              @NonNull ResponseHandler responseHandler);

    public abstract void post(@NonNull Context context, @NonNull @Client.Protocol String protocol,
                              @NonNull String url, @Nullable Map<String, String> params,
                              @NonNull ResponseHandler responseHandler);

    @Override
    public abstract boolean isAuthorized(@NonNull Context context);
}
