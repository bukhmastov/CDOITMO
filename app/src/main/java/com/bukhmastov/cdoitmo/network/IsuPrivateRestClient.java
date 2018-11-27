package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Isu;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class IsuPrivateRestClient extends Isu {

    public IsuPrivateRestClient() {
        super();
    }

    public abstract void check(@NonNull Context context, @NonNull ResponseHandler responseHandler);

    public abstract void authorize(@NonNull Context context, @NonNull ResponseHandler responseHandler);

    public abstract void authorize(@NonNull Context context, @NonNull String username, @NonNull String password, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler);

    @Override
    public abstract boolean isAuthorized(@NonNull Context context);
}
