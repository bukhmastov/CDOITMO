package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.ResponseHandlerJoiner;
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
    public void get(@NonNull Context context,
                    @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "get | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "get | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_HANDLING);
            doGet(context, getAbsoluteUrl(protocol, url), query, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    log.v(TAG, "get | url=", url, " | success | code=", code);
                    super.onSuccess(code, headers, response);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "get | url=", url, " | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
