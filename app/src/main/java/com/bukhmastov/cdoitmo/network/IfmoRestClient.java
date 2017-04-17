package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.IfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class IfmoRestClient extends Client {

    private static final String TAG = "IfmoRestClient";
    private static final String BASE_URL = "http://mountain.ifmo.ru/api.ifmo.ru/public/v1/";

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final IfmoRestClientResponseHandler responseHandler){
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.v(TAG, "get | success(JSONObject)");
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, response, null);
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    Log.v(TAG, "get | success(JSONArray)");
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, null, response);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.v(TAG, "get | failure(JSONObject)");
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.v(TAG, "get | failure(JSONArray) | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.v(TAG, "get | failure(String) | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            Log.v(TAG, "get | offline");
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

}
