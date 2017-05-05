package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class Room101Client {

    private static final String TAG = "Room101RestClient";
    private static final String BASE_URL = "https://de.ifmo.ru/m/";
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
                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        responseHandler.onSuccess(statusCode, data);
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "get | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            Log.v(TAG, "get | url=" + url + " | offline");
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
                    Log.v(TAG, "post | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        responseHandler.onSuccess(statusCode, data);
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "post | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                    responseHandler.onNewHandle(null);
                    analyseCookie(context, headers);
                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                }
            }));
        } else {
            Log.v(TAG, "post | url=" + url + " | offline");
            responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
        }
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
    private static void analyseCookie(Context context, Header[] headers){
        if (headers == null) return;
        for (Header header : headers) {
            if (Objects.equals(header.getName().toLowerCase(), "cookie") || Objects.equals(header.getName().toLowerCase(), "set-cookie")) {
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
    static String convert2UTF8(Header[] headers, byte[] content){
        try {
            if (content == null) throw new NullPointerException("content cannot be null");
            String charset = "windows-1251";
            boolean foundAtHeaders = false;
            for (Header header : headers) {
                if (Objects.equals(header.getName().toLowerCase(), "content-type")) {
                    String[] entities = header.getValue().split(";");
                    for (String entity : entities) {
                        String[] pair = entity.trim().split("=");
                        if (pair.length >= 2) {
                            if (Objects.equals(pair[0].trim().toLowerCase(), "charset")) {
                                charset = pair[1].trim().toUpperCase();
                                foundAtHeaders = true;
                            }
                        }
                        if (foundAtHeaders) break;
                    }
                    if (foundAtHeaders) break;
                }
            }
            if (!foundAtHeaders) {
                Matcher m = Pattern.compile("<meta.*charset=\"?(.*)\".*>").matcher(new String(content, "UTF-8"));
                if (m.find()) {
                    charset = m.group(1).trim().toUpperCase();
                }
            }
            if (Objects.equals(charset, "UTF-8")) {
                return new String(content, charset);
            } else {
                return new String((new String(content, charset)).getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }

}
