package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.Room101ClientResponseHandler;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class Room101Client extends Client {

    private static final String TAG = "Room101RestClient";
    private static final String BASE_URL = "de.ifmo.ru/m";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTPS;

    public static final int STATE_HANDLING = 0;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_AUTH = 2;
    public static final int FAILED_EXPECTED_REDIRECTION = 3;
    public static final int STATUS_CODE_EMPTY = -1;

    public static void get(final Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
                init();
                if (Static.isOnline(context)) {
                    responseHandler.onProgress(STATE_HANDLING);
                    renewCookieRoom101(context);
                    responseHandler.onNewHandle(checkHandle(getHttpClient().get(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Header[] headers, final byte[] responseBody) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
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
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Header[] headers, final byte[] responseBody, final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                                    responseHandler.onNewHandle(null);
                                    analyseCookie(context, headers);
                                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                                }
                            });
                        }
                    })));
                } else {
                    Log.v(TAG, "get | url=" + url + " | offline");
                    responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
                }
            }
        });
    }
    public static void post(final Context context, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void post(final Context context, final Protocol protocol, final String url, final RequestParams params, final Room101ClientResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "post | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
                init();
                if (Static.isOnline(context)) {
                    responseHandler.onProgress(STATE_HANDLING);
                    renewCookieRoom101(context);
                    responseHandler.onNewHandle(checkHandle(getHttpClient().post(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Header[] headers, final byte[] responseBody) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
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
                            });
                        }
                        @Override
                        public void onFailure(final int statusCode, final Header[] headers, final byte[] responseBody, final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "post | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                                    responseHandler.onNewHandle(null);
                                    analyseCookie(context, headers);
                                    responseHandler.onFailure(FAILED_TRY_AGAIN, statusCode, headers);
                                }
                            });
                        }
                    })));
                } else {
                    Log.v(TAG, "post | url=" + url + " | offline");
                    responseHandler.onFailure(FAILED_OFFLINE, STATUS_CODE_EMPTY, null);
                }
            }
        });
    }

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
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
    private static void renewCookieRoom101(Context context){
        httpclient.removeHeader("Cookie");
        httpclient.addHeader("User-Agent", Static.getUserAgent(context));
        httpclient.addHeader("Cookie", "PHPSESSID=" + Storage.file.perm.get(context, "user#phpsessid") + "; autoexit=true;");
        httpclientsync.removeHeader("Cookie");
        httpclientsync.addHeader("User-Agent", Static.getUserAgent(context));
        httpclientsync.addHeader("Cookie", "PHPSESSID=" + Storage.file.perm.get(context, "user#phpsessid") + "; autoexit=true;");
    }
}
