package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
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

public class DeIfmoRestClientImpl extends DeIfmoRestClient {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "de.ifmo.ru/api/private";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    DeIfmoClient deIfmoClient;

    public DeIfmoRestClientImpl() {
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
                    @NonNull String url, @Nullable Map<String, String> query,
                    @NonNull RestResponseHandler handler) {
        log.v(TAG, "get | url=", url);
        thread.assertNotUI();
        if (!Client.isOnline(context)) {
            log.v(TAG, "get | url=", url, " | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Client.Headers(null), FAILED_OFFLINE);
            return;
        }
        if (App.UNAUTHORIZED_MODE) {
            log.v(TAG, "get | UNAUTHORIZED_MODE | failed");
            handler.onFailure(STATUS_CODE_EMPTY, new Client.Headers(null), FAILED_UNAUTHORIZED_MODE);
            return;
        }
        if (isAuthExpiredByJsessionId(context)) {
            log.v(TAG, "get | auth required");
            deIfmoClient.authorize(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Client.Headers headers, String response) {
                    get(context, protocol, url, query, handler);
                }
                @Override
                public void onProgress(int state) {
                    handler.onProgress(STATE_HANDLING);
                }
                @Override
                public void onFailure(int statusCode, Client.Headers headers, int state) {
                    switch (state) {
                        case DeIfmoClient.FAILED_OFFLINE:
                        case DeIfmoClient.FAILED_SERVER_ERROR:
                        case DeIfmoClient.FAILED_INTERRUPTED:
                            break;
                        case DeIfmoClient.FAILED_TRY_AGAIN:
                        case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                        case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                            state = FAILED_TRY_AGAIN;
                            break;
                    }
                    handler.onFailure(statusCode, headers, state);
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    handler.onNewRequest(request);
                }
            });
            return;
        }
        handler.onProgress(STATE_HANDLING);
        doGetJson(context, getAbsoluteUrl(protocol, url), query, new RawJsonHandler() {
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
