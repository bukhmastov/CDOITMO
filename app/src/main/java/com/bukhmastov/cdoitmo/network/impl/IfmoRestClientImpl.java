package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IfmoRestClientImpl extends IfmoRestClient {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "mountain.ifmo.ru/api.ifmo.ru/public/v1";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    @Inject
    Log log;
    @Inject
    Thread thread;

    public IfmoRestClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Client.Protocol String protocol,
                    @NonNull String url, @Nullable  Map<String, String> query,
                    @NonNull RestResponseHandler handler) {
        log.v(TAG, "get | url=", url);
        thread.assertNotUI();
        if (!Client.isOnline(context)) {
            log.v(TAG, "get | url=", url, " | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Client.Headers(null), FAILED_OFFLINE);
            return;
        }
        handler.onProgress(STATE_HANDLING);
        gJson(context, getAbsoluteUrl(protocol, url), query, new RawJsonHandler() {
            @Override
            public void onDone(int code, okhttp3.Headers headers, String response, JSONObject obj, JSONArray arr) {
                log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                if (code >= 400) {
                    handler.onFailure(code, new Client.Headers(headers), FAILED_SERVER_ERROR);
                    return;
                }
                handler.onSuccess(code, new Client.Headers(headers), obj, arr);
            }
            @Override
            public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                invokeOnFailed(handler, code, headers, throwable, FAILED_TRY_AGAIN);
            }
            @Override
            public void onNewRequest(Client.Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Client.Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
