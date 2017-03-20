package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class DeIfmoRestClient extends Client {

    private static final String TAG = "DeIfmoRestClient";
    private static final String BASE_URL = "http://de.ifmo.ru/api/private/";

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoRestClientResponseHandler responseHandler){
        init();
        if (Static.isOnline(context)) {
            if (checkJsessionId(context)) {
                DeIfmoClient.authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        get(context, url, params, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int state) {
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
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, response, null);
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onSuccess(statusCode, null, response);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_TRY_AGAIN);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }

}
