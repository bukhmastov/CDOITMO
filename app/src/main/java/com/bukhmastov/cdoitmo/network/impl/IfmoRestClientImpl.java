package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class IfmoRestClientImpl extends IfmoRestClient {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "mountain.ifmo.ru/api.ifmo.ru/public/v1";
    private static final String DEFAULT_PROTOCOL = HTTP;

    @Inject
    Log log;
    @Inject
    Thread thread;

    public IfmoRestClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RestResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Client.Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RestResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (Client.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                gJson(context, getAbsoluteUrl(protocol, url), query, new RawJsonHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response, final JSONObject responseObj, final JSONArray responseArr) {
                        thread.run(thread.BACKGROUND, () -> {
                            log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                            if (code >= 400) {
                                responseHandler.onFailure(code, new Client.Headers(headers), FAILED_SERVER_ERROR);
                                return;
                            }
                            responseHandler.onSuccess(code, new Client.Headers(headers), responseObj, responseArr);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        thread.run(thread.BACKGROUND, () -> {
                            log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Client.Headers(headers), code >= 400 ? FAILED_SERVER_ERROR : (isCorruptedJson(throwable) ? FAILED_CORRUPTED_JSON : FAILED_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Client.Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                log.v(TAG, "get | url=", url, " | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Client.Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Client.Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
