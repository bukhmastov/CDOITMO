package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

public class Room101ClientImpl extends Room101Client {

    private static final String TAG = "Room101Client";
    private static final String BASE_URL = "de.ifmo.ru/m";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    @Override
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            Log.v(TAG, "get | url=", url);
            if (Client.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                g(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            Log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                            if (code >= 400) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                return;
                            }
                            responseHandler.onSuccess(code, new Headers(headers), response);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            Log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), (code >= 400 ? FAILED_SERVER_ERROR : FAILED_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                Log.v(TAG, "get | url=", url, " | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @Override
    public void post(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> params,@NonNull  final ResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }

    @Override
    public void post(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> params, @NonNull final ResponseHandler responseHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            Log.v(TAG, "post | url=", url);
            if (Client.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                p(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            Log.v(TAG, "post | url=", url, " | success | statusCode=", code);
                            if (code >= 400) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                return;
                            }
                            responseHandler.onSuccess(code, new Headers(headers), response);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            Log.v(TAG, "post | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), (code >= 400 ? FAILED_SERVER_ERROR : FAILED_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                Log.v(TAG, "post | url=", url, " | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @Override
    public boolean isAuthorized(@NonNull final Context context) {
        final String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
        final String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
        return !login.isEmpty() && !password.isEmpty();
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
