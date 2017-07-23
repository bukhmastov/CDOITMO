package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandlerExtended;
import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class IfmoRestClient extends Client {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "mountain.ifmo.ru/api.ifmo.ru/public/v1";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTP;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final IfmoRestClientResponseHandler responseHandler){
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final IfmoRestClientResponseHandler responseHandler){
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(protocol, url), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.v(TAG, "get | url=" + url + " | success(JSONObject) | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, response, null);
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    Log.v(TAG, "get | url=" + url + " | success(JSONArray) | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, null, response);
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Log.v(TAG, "get | url=" + url + " | success(String)(rather failure) | statusCode=" + statusCode + " | responseString=" + responseString);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Log.v(TAG, "get | url=" + url + " | failure(JSONObject) | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (errorResponse != null ? " | response=" + errorResponse.toString() : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    Log.v(TAG, "get | url=" + url + " | failure(JSONArray) | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (errorResponse != null ? " | response=" + errorResponse.toString() : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.v(TAG, "get | url=" + url + " | failure(String) | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseString != null ? " | response=" + responseString : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            Log.v(TAG, "get | url=" + url + " | offline");
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    public static void getPlain(final Context context, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        getPlain(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void getPlain(final Context context, final Protocol protocol, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        Log.v(TAG, "getPlain | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        getPlain(httpclient, context, protocol, url, params, responseHandler);
    }
    public static void getPlainSync(final Context context, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        getPlainSync(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void getPlainSync(final Context context, final Protocol protocol, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        Log.v(TAG, "getPlainSync | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        SyncHttpClient httpclient = new SyncHttpClient();
        httpclient.setLoggingLevel(android.util.Log.WARN);
        getPlain(httpclient, context, protocol, url, params, responseHandler);
    }
    private static void getPlain(final AsyncHttpClient httpclient, final Context context, final Protocol protocol, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler) {
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "getPlainSA | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        responseHandler.onSuccess(statusCode, data);
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "getPlainSA | url=" + url + " | failure | statusCode=" + statusCode + (responseBody != null ? convert2UTF8(headers, responseBody) : "") + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                    responseHandler.onNewHandle(null);
                    if (responseHandler instanceof IfmoClientResponseHandlerExtended) {
                        ((IfmoClientResponseHandlerExtended) responseHandler).onFailure(statusCode, FAILED_TRY_AGAIN);
                    } else {
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                }
            }));
        } else {
            Log.v(TAG, "getPlainSA | url=" + url + " | offline");
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }

}
