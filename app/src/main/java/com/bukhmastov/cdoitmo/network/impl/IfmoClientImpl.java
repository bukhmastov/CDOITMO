package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IfmoClientImpl extends IfmoClient {

    private static final String TAG = "IfmoClient";
    private static final String BASE_URL = "www.ifmo.ru";
    private static final String DEFAULT_PROTOCOL = HTTP;

    @Inject
    Log log;
    @Inject
    Thread thread;

    public IfmoClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Protocol String protocol,
                    @NonNull String url, @Nullable Map<String, String> query,
                    @NonNull ResponseHandler handler) {
        log.v(TAG, "get | url=", url);
        thread.assertNotUI();
        if (!Client.isOnline(context)) {
            log.v(TAG, "get | url=", url, " | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            return;
        }
        handler.onProgress(STATE_HANDLING);
        doGet(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
            @Override
            public void onDone(int code, okhttp3.Headers headers, String response) throws Exception {
                log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                if (code >= 400) {
                    handler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                    return;
                }
                handler.onSuccess(code, new Headers(headers), response);
            }
            @Override
            public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                invokeOnFailed(handler, code, headers, throwable, FAILED_TRY_AGAIN);
            }
            @Override
            public void onNewRequest(Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
