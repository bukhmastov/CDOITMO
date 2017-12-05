package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Ifmo;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class IfmoRestClient extends Ifmo {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "mountain.ifmo.ru/api.ifmo.ru/public/v1";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTP;

    public static void get(final Context context, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "get | url=" + url);
                if (Static.isOnline(context)) {
                    responseHandler.onProgress(STATE_HANDLING);
                    gJson(context, getAbsoluteUrl(protocol, url), query, new RawJsonHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response, final JSONObject responseObj, final JSONArray responseArr) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + code);
                                    if (code >= 500 && code < 600) {
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                        return;
                                    }
                                    responseHandler.onSuccess(code, new Headers(headers), responseObj, responseArr);
                                }
                            });
                        }
                        @Override
                        public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
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
