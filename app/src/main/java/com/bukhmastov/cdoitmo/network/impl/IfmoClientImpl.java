package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

import javax.inject.Inject;

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
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (Client.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                g(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        thread.run(thread.BACKGROUND, () -> {
                            log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                            if (code >= 400) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                return;
                            }
                            responseHandler.onSuccess(code, new Headers(headers), response);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        thread.run(thread.BACKGROUND, () -> {
                            log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), (code >= 400 ? FAILED_SERVER_ERROR : FAILED_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                log.v(TAG, "get | url=", url, " | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
