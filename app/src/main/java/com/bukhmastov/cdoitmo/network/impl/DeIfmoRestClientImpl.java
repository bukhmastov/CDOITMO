package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.App;
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

public class DeIfmoRestClientImpl extends DeIfmoRestClient {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "de.ifmo.ru/api/private";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    //@Inject
    private DeIfmoClient deIfmoClient = DeIfmoClient.instance();

    @Override
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RestResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Client.Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RestResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (Client.isOnline(context)) {
                if (App.UNAUTHORIZED_MODE) {
                    log.v(TAG, "get | UNAUTHORIZED_MODE | failed");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Client.Headers(null), FAILED_UNAUTHORIZED_MODE);
                    return;
                }
                if (checkJsessionId(context)) {
                    log.v(TAG, "get | auth required");
                    deIfmoClient.authorize(context, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Client.Headers headers, String response) {
                            get(context, protocol, url, query, responseHandler);
                        }
                        @Override
                        public void onProgress(int state) {
                            responseHandler.onProgress(STATE_HANDLING);
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            switch (state) {
                                case DeIfmoClient.FAILED_OFFLINE:
                                    state = FAILED_OFFLINE;
                                    break;
                                case DeIfmoClient.FAILED_SERVER_ERROR:
                                    state = FAILED_SERVER_ERROR;
                                    break;
                                case DeIfmoClient.FAILED_INTERRUPTED:
                                    state = FAILED_INTERRUPTED;
                                    break;
                                case DeIfmoClient.FAILED_TRY_AGAIN:
                                case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                                case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                    state = FAILED_TRY_AGAIN;
                                    break;
                            }
                            responseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                    return;
                }
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
                            responseHandler.onFailure(code, new Client.Headers(headers), isInterrupted(throwable) ? FAILED_INTERRUPTED : (code >= 400 ? FAILED_SERVER_ERROR : (isCorruptedJson(throwable) ? FAILED_CORRUPTED_JSON : FAILED_TRY_AGAIN)));
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
