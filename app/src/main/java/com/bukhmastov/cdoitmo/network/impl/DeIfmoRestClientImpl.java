package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestStringResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

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
    public <T extends JsonEntity> void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public <T extends JsonEntity> void get(@NonNull Context context,
                    @NonNull @Client.Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        try {
            log.v(TAG, "get | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "get | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            if (App.UNAUTHORIZED_MODE) {
                log.v(TAG, "get | url=", url, " | denied | non-authorized mode");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_DENIED);
                return;
            }
            if (isAuthExpiredByJsessionId(context)) {
                log.v(TAG, "get | url=", url, " | auth required");
                deIfmoClient.authorize(context, new RestStringResponseHandlerJoiner(handler) {
                    @Override
                    public void onSuccess(int code, Headers headers, String response) throws Exception {
                        log.v(TAG, "get | url=", url, " | auth recovered");
                        get(context, protocol, url, query, handler);
                    }
                    @Override
                    public void onFailure(int code, Headers headers, int state) {
                        log.v(TAG, "get | url=", url, " | failed to recover auth");
                        super.onFailure(code, headers, state);
                    }
                });
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
                    log.v(TAG, "get | url=", url, " | failure | code=", code, " | state=", state);
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
