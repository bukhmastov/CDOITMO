package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.IfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class IfmoClient extends Client {

    private static final String TAG = "IfmoClient";
    private static final String BASE_URL = "http://www.ifmo.ru/";

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;

    public static void get(final Context context, final String url, final RequestParams params, final IfmoClientResponseHandler responseHandler){
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    try {
                        if (responseBody == null) throw new NullPointerException("responseBody cannot be null");
                        String data;
                        String charset = "windows-1251";
                        Matcher m = Pattern.compile("<meta.*charset=\"?(.*)\".*>").matcher(new String(responseBody, "UTF-8"));
                        if (m.find()) charset = m.group(1).toUpperCase();
                        if (Objects.equals(charset, "UTF-8")) {
                            data = new String(responseBody, charset);
                        } else {
                            data = new String((new String(responseBody, charset)).getBytes("UTF-8"));
                        }
                        responseHandler.onSuccess(statusCode, data);
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
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
