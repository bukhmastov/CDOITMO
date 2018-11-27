package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.PropertiesUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Isu extends Client {

    public static final int STATE_CHECKING = 10;
    public static final int STATE_AUTHORIZATION = 11;
    public static final int STATE_AUTHORIZED = 12;
    public static final int FAILED_AUTH_TRY_AGAIN = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;

    public Isu() {
        super();
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param params of request
     * @param rawHandler of request, cannot be null
     * @see RawHandler
     */
    protected void doPost(@NonNull final Context context, @NonNull final String url, @Nullable Map<String, String> query, @Nullable final Map<String, String> params, @NonNull final RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            doPost(url, getHeaders(context), query, params, rawHandler);
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
            doGetJson(getUrl(context, url), getHeaders(context), query, rawJsonHandler);
        }, throwable -> {
            rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
        });
    }

    @NonNull
    private okhttp3.Headers getHeaders(@NonNull final Context context) throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", PropertiesUtils.getIsuProperty(context, "isu.api.user.agent"));
        return okhttp3.Headers.of(headers);
    }

    private String getUrl(@NonNull Context context, @NonNull String url) throws IOException {
        url = url.replace("%apikey%", PropertiesUtils.getIsuProperty(context, "isu.api.key"));
        if (url.contains("%isutoken%")) {
            url = url.replace("%isutoken%", storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", ""));
        }
        return url;
    }
}
