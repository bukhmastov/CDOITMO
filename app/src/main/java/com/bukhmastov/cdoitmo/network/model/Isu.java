package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.PropertiesUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Isu extends Client {

    public Isu() {
        super();
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param params of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doPost(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query,
                          @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        try {
            doPost(url, getHeaders(context), query, params, handler);
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs GET request and parse result as {@link JsonEntity}
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param query of request
     * @param restHandler of request, cannot be null
     * @see RestResponseHandler
     */
    protected <T extends JsonEntity> void doGetJson(@NonNull Context context, @NonNull String url,
                        @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> restHandler) {
        try {
            doGetJson(getUrl(context, url), getHeaders(context), query, restHandler);
        } catch (Exception exception) {
            restHandler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    protected Integer getFailedState(@NonNull Context context, int code) {
        if (code == 401) {
            return FAILED_AUTH_REQUIRED;
        }
        return super.getFailedState(context, code);
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
