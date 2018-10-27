package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Room101;

import java.util.Map;

public abstract class Room101Client extends Room101 {

    public Room101Client() {
        super();
    }

    public abstract void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler);

    public abstract void post(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    public abstract void post(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler);

    @Override
    public abstract boolean isAuthorized(@NonNull Context context);
}
