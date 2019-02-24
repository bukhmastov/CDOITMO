package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        try {
            doGet(url, getHeaders(context), query, handler);
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    /**
     * Performs POST request
     * @param context context, cannot be null
     * @param url to be requested, cannot be null
     * @param params of request
     * @param handler of request, cannot be null
     * @see ResponseHandler
     */
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        try {
            doPost(url, getHeaders(context), null, params, handler);
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
            doGetJson(url, getHeaders(context), query, restHandler);
        } catch (Exception exception) {
            restHandler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @NonNull
    private okhttp3.Headers getHeaders(@NonNull final Context context) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", networkUserAgentProvider.get(context));
        return okhttp3.Headers.of(headers);
    }
}
