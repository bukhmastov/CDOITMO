package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.DeIfmo;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class DeIfmoRestClient extends DeIfmo {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "de.ifmo.ru/api/private";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTPS;

    public static void get(final Context context, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            Log.v(TAG, "get | url=", url);
            if (Static.isOnline(context)) {
                if (Static.UNAUTHORIZED_MODE) {
                    Log.v(TAG, "get | UNAUTHORIZED_MODE | failed");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_UNAUTHORIZED_MODE);
                    return;
                }
                if (checkJsessionId(context)) {
                    Log.v(TAG, "get | auth required");
                    DeIfmoClient.authorize(context, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, String response) {
                            get(context, protocol, url, query, responseHandler);
                        }
                        @Override
                        public void onProgress(int state) {
                            responseHandler.onProgress(STATE_HANDLING);
                        }
                        @Override
                        public void onFailure(int statusCode, Headers headers, int state) {
                            switch (state) {
                                case DeIfmoClient.FAILED_OFFLINE:
                                    state = FAILED_OFFLINE;
                                    break;
                                case DeIfmoClient.FAILED_SERVER_ERROR:
                                    state = FAILED_SERVER_ERROR;
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
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                    return;
                }
                responseHandler.onProgress(STATE_HANDLING);
                gJson(context, getAbsoluteUrl(protocol, url), query, new RawJsonHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response, final JSONObject responseObj, final JSONArray responseArr) {
                        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                            Log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                            if (code >= 400) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                return;
                            }
                            responseHandler.onSuccess(code, new Headers(headers), responseObj, responseArr);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                            Log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), code >= 400 ? FAILED_SERVER_ERROR : (isCorruptedJson(throwable) ? FAILED_CORRUPTED_JSON : FAILED_TRY_AGAIN));
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

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
