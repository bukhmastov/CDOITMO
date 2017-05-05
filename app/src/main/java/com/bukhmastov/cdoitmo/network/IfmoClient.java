package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class IfmoClient extends Client {

    private static final String TAG = "IfmoClient";
    private static final String BASE_URL = "http://www.ifmo.ru/";

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + statusCode);
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

                    Log.v(TAG, "get | url=" + url + " | failure | statusCode=" + statusCode + (responseBody != null ? convert2UTF8(headers, responseBody) : "") + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            Log.v(TAG, "get | url=" + url + " | offline");
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

}
