package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Ifmo;

import java.util.Map;

public abstract class IfmoClient extends Ifmo {

    public IfmoClient() {
        super();
    }

    public abstract void get(@NonNull Context context, @NonNull String url,
                             @Nullable Map<String, String> query, @NonNull ResponseHandler handler);

    public abstract void get(@NonNull Context context, @NonNull @Protocol String protocol,
                             @NonNull String url, @Nullable Map<String, String> query,
                             @NonNull ResponseHandler handler);
}
