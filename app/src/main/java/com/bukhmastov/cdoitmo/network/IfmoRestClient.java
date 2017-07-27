package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class IfmoRestClient extends Client {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "mountain.ifmo.ru/api.ifmo.ru/public/v1";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTP;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final IfmoRestClientResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final IfmoRestClientResponseHandler responseHandler) {
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(checkHandle(getHttpClient().get(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        Log.v(TAG, "get | url=" + url + " | success | statusCode=" + statusCode);
                        responseHandler.onNewHandle(null);
                        String response = convert2UTF8(headers, responseBody);
                        if (response == null) throw new NullPointerException("response cannot be null");
                        response = response.trim();
                        if (response.startsWith("{") && response.endsWith("}")) {
                            Log.v(TAG, "get | url=" + url + " | success(JSONObject) | statusCode=" + statusCode);
                            JSONObject jsonObject;
                            try {
                                try {
                                    jsonObject = new JSONObject(response);
                                } catch (Throwable throwable) {
                                    Log.v(TAG, "get | url=" + url + " | success(JSONObject) -> going to parse invalid json");
                                    jsonObject = new JSONObject(fixInvalidResponse(response));
                                    Log.v(TAG, "get | url=" + url + " | success(JSONObject) -> invalid json parsed");
                                }
                            } catch (Throwable throwable) {
                                Log.v(TAG, "get | url=" + url + " | success(JSONObject) -> failed to parse json");
                                jsonObject = null;
                            }
                            if (jsonObject != null) {
                                responseHandler.onSuccess(statusCode, jsonObject, null);
                            } else {
                                responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                            }
                        } else if (response.startsWith("[") && response.endsWith("]")) {
                            Log.v(TAG, "get | url=" + url + " | success(JSONArray) | statusCode=" + statusCode);
                            JSONArray jsonArray;
                            try {
                                try {
                                    jsonArray = new JSONArray(response);
                                } catch (Throwable throwable) {
                                    Log.v(TAG, "get | url=" + url + " | success(JSONArray) -> going to parse invalid json");
                                    jsonArray = new JSONArray(fixInvalidResponse(response));
                                    Log.v(TAG, "get | url=" + url + " | success(JSONArray) -> invalid json parsed");
                                }
                            } catch (Throwable throwable) {
                                Log.v(TAG, "get | url=" + url + " | success(JSONArray) -> failed to parse json");
                                jsonArray = null;
                            }
                            if (jsonArray != null) {
                                responseHandler.onSuccess(statusCode, null, jsonArray);
                            } else {
                                responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                            }
                        } else {
                            Log.v(TAG, "get | url=" + url + " | success(failure) | statusCode=" + statusCode + " | response=" + response);
                            responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "get | url=" + url + " | failure | statusCode=" + statusCode + (responseBody != null ? convert2UTF8(headers, responseBody) : "") + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                }
            })));
        } else {
            Log.v(TAG, "get | url=" + url + " | offline");
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_OFFLINE);
        }
    }

    private static String fixInvalidResponse(String response) {
        Matcher m = Pattern.compile("(\\\\u)([0-9a-f]{3})[^0-9a-f]").matcher(response);
        if (m.find()) {
            response = m.replaceAll(m.group(1) + "0" + m.group(2));
        }
        return response;
    }
    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
