package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.DeIfmo;

import java.util.Map;

public abstract class DeIfmoRestClient extends DeIfmo {

    public DeIfmoRestClient() {
        super();
    }

    public abstract <T extends JsonEntity> void get(@NonNull Context context, @NonNull String url,
                            @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler);

    public abstract <T extends JsonEntity> void get(@NonNull Context context,
                            @NonNull @Client.Protocol String protocol, @NonNull String url,
                            @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler);
}
