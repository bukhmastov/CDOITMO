package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Ifmo;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Map;

public class IfmoClient extends Ifmo {

    private static final String TAG = "IfmoClient";
    private static final String BASE_URL = "www.ifmo.ru";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTP;

    public static void get(final Context context, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "get | url=" + url);
                if (Static.isOnline(context)) {
                    responseHandler.onProgress(STATE_HANDLING);
                    g(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + code);
                                    responseHandler.onSuccess(code, new Headers(headers), response);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_TRY_AGAIN);
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                } else {
                    Log.v(TAG, "get | url=" + url + " | offline");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                }
            }
        });
    }

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
