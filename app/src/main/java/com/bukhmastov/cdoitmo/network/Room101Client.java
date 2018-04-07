package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Room101;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.Map;

public class Room101Client extends Room101 {

    private static final String TAG = "Room101Client";
    private static final String BASE_URL = "de.ifmo.ru/m";
    private static final String DEFAULT_PROTOCOL = HTTPS;

    public static void get(final Context context, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }
    public static void get(final Context context, final @Protocol String protocol, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            Log.v(TAG, "get | url=", url);
            if (Static.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                g(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Static.T.runThread(Static.T.BACKGROUND, () -> {
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
                        Static.T.runThread(Static.T.BACKGROUND, () -> {
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
    public static void post(final Context context, final String url, final Map<String, String> params, final ResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void post(final Context context, final @Protocol String protocol, final String url, final Map<String, String> params, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            Log.v(TAG, "post | url=", url);
            if (Static.isOnline(context)) {
                responseHandler.onProgress(STATE_HANDLING);
                p(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Static.T.runThread(Static.T.BACKGROUND, () -> {
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
                        Static.T.runThread(Static.T.BACKGROUND, () -> {
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

    public static boolean isAuthorized(final Context context) {
        final String login = Storage.file.perm.get(context, "user#deifmo#login", "").trim();
        final String password = Storage.file.perm.get(context, "user#deifmo#password", "").trim();
        return !login.isEmpty() && !password.isEmpty();
    }

    private static String getAbsoluteUrl(@Protocol String protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
