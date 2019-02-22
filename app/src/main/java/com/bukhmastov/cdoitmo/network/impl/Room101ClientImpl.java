package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.Map;

import javax.inject.Inject;

public class Room101ClientImpl extends Room101Client {

    private static final String TAG = "Room101Client";
    private static final String BASE_URL = "de.ifmo.ru/m";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;

    public Room101ClientImpl() {
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

    @Override
    public void post(@NonNull Context context, @NonNull String url,
                     @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        post(context, DEFAULT_PROTOCOL, url, params, handler);
    }

    @Override
    public void post(@NonNull Context context, @NonNull @Protocol String protocol,
                     @NonNull String url, @Nullable Map<String, String> params,
                     @NonNull ResponseHandler handler) {
        log.v(TAG, "post | url=", url);
        thread.assertNotUI();
        if (!Client.isOnline(context)) {
            log.v(TAG, "post | url=", url, " | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            return;
        }
        handler.onProgress(STATE_HANDLING);
        doPost(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
            @Override
            public void onDone(int code, okhttp3.Headers headers, String response) throws Exception {
                log.v(TAG, "post | url=", url, " | success | statusCode=", code);
                if (code >= 400) {
                    handler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                    return;
                }
                handler.onSuccess(code, new Headers(headers), response);
            }
            @Override
            public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                log.v(TAG, "post | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                invokeOnFailed(handler, code, headers, throwable, FAILED_TRY_AGAIN);
            }
            @Override
            public void onNewRequest(Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    @Override
    public boolean isAuthorized(@NonNull final Context context) {
        thread.assertNotUI();
        final String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
        final String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
        return StringUtils.isNotEmpty(login) && StringUtils.isNotEmpty(password);
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
