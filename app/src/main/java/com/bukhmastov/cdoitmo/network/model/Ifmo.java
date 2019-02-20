package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public abstract class Ifmo extends Client {

    @Inject
    NetworkUserAgentProvider networkUserAgentProvider;

    public Ifmo() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    /**
     * Performs GET request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull RawHandler rawHandler) {
        try {
            doGet(url, getHeaders(context), query, rawHandler);
        } catch (Throwable throwable) {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        }
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param params of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull RawHandler rawHandler) {
        try {
            doPost(url, getHeaders(context), null, params, rawHandler);
        } catch (Throwable throwable) {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        }
    }

    /**
     * Performs GET request and parse result as json
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param rawJsonHandler of request, cannot be null
     * @see RawJsonHandler
     */
    protected void gJson(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull RawJsonHandler rawJsonHandler) {
        try {
            doGetJson(url, getHeaders(context), query, rawJsonHandler);
        } catch (Throwable throwable) {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        }
    }

    @NonNull
    private okhttp3.Headers getHeaders(@NonNull final Context context) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(context));
        return okhttp3.Headers.of(headers);
    }
}
