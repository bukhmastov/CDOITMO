package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101Client {

    private static final String TAG = "Room101RestClient";
    private static final String BASE_URL = "http://de.ifmo.ru/m/";
    private static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_AUTH = 2;
    public static final int FAILED_EXPECTED_REDIRECTION = 3;
    public static final int STATUS_CODE_EMPTY = -1;

    private static void init(){
        if (!initialized) {
            initialized = true;
            httpclient.setLoggingLevel(android.util.Log.WARN);
        }
    }

    public static void get(final Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler){
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "get | success");
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    try {
                        String data = "";
                        if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        responseHandler.onSuccess(statusCode, data);
                    } catch (UnsupportedEncodingException e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.v(TAG, "get | failure | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            Log.v(TAG, "get | offline");
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }
    public static void post(final Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler){
        Log.v(TAG, "post | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "post | success");
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    try {
                        String data = "";
                        if (responseBody != null) data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        responseHandler.onSuccess(statusCode, data);
                    } catch (UnsupportedEncodingException e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.v(TAG, "post | failure | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            Log.v(TAG, "post | offline");
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
    private static void analyseCookie(Context context, Header[] headers){
        if (headers == null) return;
        for (Header header : headers) {
            if (Objects.equals(header.getName(), "Cookie") || Objects.equals(header.getName(), "Set-Cookie")) {
                String[] pairs = header.getValue().trim().split(";");
                for (String pair : pairs) {
                    String[] cookie = pair.split("=");
                    if (Objects.equals(cookie[0], "PHPSESSID") && !Objects.equals(cookie[1], "") && cookie[1] != null) {
                        Storage.file.perm.put(context, "user#phpsessid", cookie[1]);
                    }
                }
            }
        }
    }
    private static void renewCookie(Context context){
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("User-Agent", Static.getUserAgent(context));
        httpclient.addHeader("Cookie", "PHPSESSID=" + Storage.file.perm.get(context, "user#phpsessid") + "; autoexit=true;");
    }
}
