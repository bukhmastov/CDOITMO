package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class DeIfmoRestClient extends Client {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "de.ifmo.ru/api/private";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTPS;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoRestClientResponseHandler responseHandler){
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final DeIfmoRestClientResponseHandler responseHandler){
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            if (checkJsessionId(context)) {
                Log.v(TAG, "get | auth required");
                DeIfmoClient.authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        get(context, url, params, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(STATE_HANDLING);
                    }
                    @Override
                    public void onFailure(int state) {
                        switch (state) {
                            case DeIfmoClient.FAILED_OFFLINE:
                                state = FAILED_OFFLINE;
                                break;
                            case DeIfmoClient.FAILED_TRY_AGAIN:
                            case DeIfmoClient.FAILED_AUTH_TRY_AGAIN:
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                            case DeIfmoClient.FAILED_AUTH_CREDENTIALS_FAILED:
                                state = FAILED_TRY_AGAIN;
                                break;
                        }
                        responseHandler.onFailure(state);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
                return;
            }
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

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }

}
