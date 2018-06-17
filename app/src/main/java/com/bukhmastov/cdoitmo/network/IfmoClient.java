package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.Ifmo;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

public class IfmoClient extends Ifmo {

    private static final String TAG = "IfmoClient";
    private static final String BASE_URL = "www.ifmo.ru";
    private static final String DEFAULT_PROTOCOL = HTTP;

    public static void get(final Context context, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }
    public static void get(final Context context, final @Protocol String protocol, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
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

    private static String getAbsoluteUrl(@Protocol String protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
