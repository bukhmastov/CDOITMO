package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Isu;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class IsuRestClient extends Isu {

    public IsuRestClient() {
        super();
    }

    public abstract void get(@NonNull Context context, @NonNull String url,
                             @Nullable Map<String, String> query, @NonNull RestResponseHandler handler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol,
                             @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull RestResponseHandler handler);
}
