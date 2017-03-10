package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.util.Log;

import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101Client {

    private static final String TAG = "Room101RestClient";
    private static final String BASE_URL = "http://de.ifmo.ru/m/";
    private static final String USER_AGENT = "Android Application";
    private static AsyncHttpClient httpclient = new AsyncHttpClient();
    private static boolean initialized = false;
    private static String PHPSESSID = "";

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_AUTH = 2;
    public static final int FAILED_EXPECTED_REDIRECTION = 3;
    public static final int STATUS_CODE_EMPTY = -1;

    private static void init(){
        if (!initialized) {
            initialized = true;
            httpclient.setLoggingLevel(Log.WARN);
            httpclient.addHeader("User-Agent", USER_AGENT);
            httpclient.addHeader("Cookie", "PHPSESSID=" + PHPSESSID + "; autoexit=true;");
        }
    }

    public static void get(Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler){
        init();
        if(Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie();
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
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
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }
    public static void post(Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler){
        init();
        if(Static.isOnline(context)) {
            responseHandler.onProgress(STATE_HANDLING);
            renewCookie();
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
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
                    responseHandler.onNewHandle(null);
                    analyseCookie(headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
    private static void renewCookie(){
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("Cookie", "PHPSESSID=" + PHPSESSID + "; autoexit=true;");
    }
    private static void analyseCookie(Header[] headers){
        if (headers == null) return;
        for (Header header : headers) {
            if (Objects.equals(header.getName(), "Cookie") || Objects.equals(header.getName(), "Set-Cookie")) {
                String[] pairs = header.getValue().trim().split(";");
                for (String pair : pairs) {
                    String[] cookie = pair.split("=");
                    if (Objects.equals(cookie[0], "PHPSESSID") && !Objects.equals(cookie[1], "") && cookie[1] != null) PHPSESSID = cookie[1];
                }
            }
        }
    }

}
