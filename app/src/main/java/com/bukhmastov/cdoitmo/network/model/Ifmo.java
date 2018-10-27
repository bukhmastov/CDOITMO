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
    protected void g(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            doGet(url, getHeaders(context), query, rawHandler);
        }, throwable -> {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param params of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void p(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> params, @NonNull final RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            doPost(url, getHeaders(context), null, params, rawHandler);
        }, throwable -> {
            rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    /**
     * Performs GET request and parse result as json
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param rawJsonHandler of request, cannot be null
     * @see RawJsonHandler
     */
    protected void gJson(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RawJsonHandler rawJsonHandler) {
        thread.run(thread.BACKGROUND, () -> {
            doGetJson(url, getHeaders(context), query, rawJsonHandler);
        }, throwable -> {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    @NonNull
    private okhttp3.Headers getHeaders(@NonNull final Context context) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(context));
        return okhttp3.Headers.of(headers);
    }
}
