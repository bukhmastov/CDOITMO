package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IsuRestClientImpl extends IsuRestClient {

    private static final String TAG = "IsuRestClient";
    private static final String BASE_URL = "isu.ifmo.ru/ords/isurest/v1/api/core";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    @Inject
    Log log;
    @Inject
    Thread thread;

    public IsuRestClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public <T extends JsonEntity> void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public <T extends JsonEntity> void get(@NonNull Context context,
                    @NonNull String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        try {
            log.v(TAG, "get | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "get | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_HANDLING);
            doGetJson(context, getAbsoluteUrl(protocol, url), query, new RestResponseHandlerJoiner<T>(handler) {
                @Override
                public void onSuccess(int code, Headers headers, T response) throws Exception {
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
    private String getAbsoluteUrl(@NonNull @Client.Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
