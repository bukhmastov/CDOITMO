package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Ifmo;

import java.util.Map;

public abstract class IfmoRestClient extends Ifmo {

    public IfmoRestClient() {
        super();
    }

    public abstract void get(@NonNull Context context, @NonNull String url,
                             @Nullable Map<String, String> query, @NonNull RestResponseHandler handler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol,
                             @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull RestResponseHandler handler);
}
