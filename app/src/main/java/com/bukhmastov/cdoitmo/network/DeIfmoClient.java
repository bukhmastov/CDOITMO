package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoDrawableClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.UserDataParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class DeIfmoClient extends Client {

    private static final String TAG = "DeIfmoClient";
    private static final String BASE_URL = "de.ifmo.ru";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTPS;
    private static final boolean DEFAULT_RE_AUTH = true;

    public static final int STATE_CHECKING = 0;
    public static final int STATE_AUTHORIZATION = 1;
    public static final int STATE_AUTHORIZED = 2;
    public static final int STATE_HANDLING = 3;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_AUTH_TRY_AGAIN = 2;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 3;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 4;

    public static void check(final Context context, final DeIfmoClientResponseHandler responseHandler) {
        Log.v(TAG, "check");
        init();
        if (Static.isOnline(context)) {
            responseHandler.onProgress(STATE_CHECKING);
            if (Storage.file.perm.get(context, "user#jsessionid").isEmpty() || checkJsessionId(context)) {
                authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        check(context, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int statusCode, int state) {
                        responseHandler.onFailure(statusCode, state);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            } else {
                get(context, "servlet/distributedCDE?Rule=editPersonProfile", null, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, final String response) {
                        Log.v(TAG, "check | success | going to parse user data");
                        new UserDataParse(new UserDataParse.response() {
                            @Override
                            public void finish(HashMap<String, String> result) {
                                if (result != null) {
                                    Log.v(TAG, "check | success | parsed");
                                    Storage.file.perm.put(context, "user#name", result.get("name"));
                                    Storage.file.perm.put(context, "user#group", result.get("group"));
                                    Storage.file.perm.put(context, "user#avatar", result.get("avatar"));
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("week", Integer.parseInt(result.get("week")));
                                        Storage.file.general.put(context, "user#week", jsonObject.toString());
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Storage.file.general.delete(context, "user#week");
                                    }
                                    FirebaseAnalyticsProvider.setUserProperties(context, result.get("group"));
                                    responseHandler.onSuccess(200, "");
                                } else {
                                    Log.v(TAG, "check | success | not parsed");
                                    responseHandler.onSuccess(200, "");
                                }
                            }
                        }).execute(response);
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int statusCode, int state) {
                        Log.v(TAG, "check | failed | statusCode = " + statusCode + " |state = " + state);
                        responseHandler.onFailure(statusCode, state);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            }
        } else {
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_OFFLINE);
        }
    }
    public static void authorize(final Context context, final DeIfmoClientResponseHandler responseHandler) {
        Log.v(TAG, "authorize");
        init();
        responseHandler.onProgress(STATE_AUTHORIZATION);
        String login = Storage.file.perm.get(context, "user#login");
        String password = Storage.file.perm.get(context, "user#password");
        if (Objects.equals(login, "") || Objects.equals(password, "")) {
            Log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_AUTH_CREDENTIALS_REQUIRED);
        } else {
            RequestParams params = new RequestParams();
            params.put("Rule", "LOGON");
            params.put("LOGIN", login);
            params.put("PASSWD", password);
            renewCookie(context);
            responseHandler.onNewHandle(checkHandle(getHttpClient().post(getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "authorize | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    for (Header header : headers) {
                        if (Objects.equals(header.getName().toLowerCase(), "set-cookie")) {
                            String[] pairs = header.getValue().split(";");
                            for (String pair : pairs) {
                                String[] cookie = pair.split("=");
                                if (Objects.equals(cookie[0], "JSESSIONID")) {
                                    if (!Objects.equals(cookie[1], "") && cookie[1] != null) {
                                        Storage.file.perm.put(context, "user#jsessionid", cookie[1]);
                                    } else {
                                        Log.e(TAG, "authorize | success | got 'Set-Cookie' header with empty 'JSESSIONID'");
                                        responseHandler.onFailure(statusCode, FAILED_AUTH_TRY_AGAIN);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        if (data.contains("Access is forbidden") && data.contains("Invalid login/password")) {
                            Log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                            responseHandler.onFailure(statusCode, FAILED_AUTH_CREDENTIALS_FAILED);
                        } else if (data.contains("Выбор группы безопасности") && data.contains("OPTION VALUE=8")) {
                            Log.v(TAG, "authorize | success | going to select security group");
                            getHttpClient().get(getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + Storage.file.perm.get(context, "user#login") + "&SECURITYGROUP=8&COMPNAME="), null, new AsyncHttpResponseHandler(){
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                    Log.v(TAG, "authorize | success | security group | authorized | statusCode=" + statusCode);
                                    responseHandler.onProgress(STATE_AUTHORIZED);
                                    responseHandler.onSuccess(statusCode, "authorized");
                                }
                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable throwable) {
                                    Log.v(TAG, "authorize | success | security group | FAILED_AUTH_TRY_AGAIN | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (errorResponse != null ? " | response=" + convert2UTF8(headers, errorResponse) : ""));
                                    responseHandler.onFailure(statusCode, FAILED_AUTH_TRY_AGAIN);
                                }
                            });
                        } else if (data.contains("Обучение и аттестация")) {
                            Log.v(TAG, "authorize | success | authorized");
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(statusCode, "authorized");
                        } else {
                            Log.v(TAG, "authorize | success | FAILED_AUTH_TRY_AGAIN");
                            responseHandler.onFailure(statusCode, FAILED_AUTH_TRY_AGAIN);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(statusCode, FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "authorize | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(statusCode, FAILED_AUTH_TRY_AGAIN);
                }
            })));
        }
    }
    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler, DEFAULT_RE_AUTH);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler) {
        get(context, protocol, url, params, responseHandler, DEFAULT_RE_AUTH);
    }
    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler, final boolean reAuth) {
        get(context, DEFAULT_PROTOCOL, url, params, responseHandler, reAuth);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler, final boolean reAuth) {
        Log.v(TAG, "get | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            if (reAuth && checkJsessionId(context)) {
                authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        get(context, url, params, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int statusCode, int state) {
                        responseHandler.onFailure(statusCode, state);
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
            responseHandler.onNewHandle(checkHandle(getHttpClient().get(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
                            Log.v(TAG, "get | url=" + url + " | success | auth required");
                            if (reAuth) {
                                authorize(context, new DeIfmoClientResponseHandler() {
                                    @Override
                                    public void onSuccess(int statusCode, String response) {
                                        get(context, url, params, responseHandler);
                                    }
                                    @Override
                                    public void onProgress(int state) {
                                        responseHandler.onProgress(state);
                                    }
                                    @Override
                                    public void onFailure(int statusCode, int state) {
                                        responseHandler.onFailure(statusCode, state);
                                    }
                                    @Override
                                    public void onNewHandle(RequestHandle requestHandle) {
                                        responseHandler.onNewHandle(requestHandle);
                                    }
                                });
                            } else {
                                responseHandler.onFailure(statusCode, FAILED_AUTH_CREDENTIALS_REQUIRED);
                            }
                        } else {
                            responseHandler.onSuccess(statusCode, data);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "get | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                }
            })));
        } else {
            Log.v(TAG, "get | offline");
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_OFFLINE);
        }
    }
    public static void post(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void post(final Context context, final Protocol protocol, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler) {
        Log.v(TAG, "post | url=" + url + " | params=" + Static.getSafetyRequestParams(params));
        init();
        if (Static.isOnline(context)) {
            if (checkJsessionId(context)) {
                authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        post(context, url, params, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int statusCode, int state) {
                        responseHandler.onFailure(statusCode, state);
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
            responseHandler.onNewHandle(checkHandle(getHttpClient().post(getAbsoluteUrl(protocol, url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.v(TAG, "post | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    try {
                        String data = convert2UTF8(headers, responseBody);
                        if (data == null) throw new NullPointerException("data cannot be null");
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
                            Log.v(TAG, "post | success | auth required");
                            authorize(context, new DeIfmoClientResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, String response) {
                                    post(context, url, params, responseHandler);
                                }
                                @Override
                                public void onProgress(int state) {
                                    responseHandler.onProgress(state);
                                }
                                @Override
                                public void onFailure(int statusCode, int state) {
                                    responseHandler.onFailure(statusCode, state);
                                }
                                @Override
                                public void onNewHandle(RequestHandle requestHandle) {
                                    responseHandler.onNewHandle(requestHandle);
                                }
                            });
                        } else {
                            responseHandler.onSuccess(statusCode, data);
                        }
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable throwable) {
                    Log.v(TAG, "post | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (responseBody != null ? " | response=" + convert2UTF8(headers, responseBody) : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                }
            })));
        } else {
            Log.v(TAG, "post | offline");
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_OFFLINE);
        }
    }
    public static void getAvatar(final Context context, final String url, final DeIfmoDrawableClientResponseHandler responseHandler) {
        getAvatar(context, Protocol.HTTPS, url, responseHandler);
    }
    public static void getAvatar(final Context context, final Protocol protocol, final String url, final DeIfmoDrawableClientResponseHandler responseHandler) {
        Log.v(TAG, "getAvatar | url=" + url);
        init();
        if (Static.isOnline(context)) {
            if (checkJsessionId(context)) {
                authorize(context, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {
                        getAvatar(context, protocol, url, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int statusCode, int state) {
                        responseHandler.onFailure(statusCode, state);
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
            String[] allowed = {
                    "application/octet-stream",
                    "image/jpeg", "image/png", "image/gif",
                    "image/jpeg;charset=Windows-1251", "image/png;charset=Windows-1251", "image/gif;charset=Windows-1251"
            };
            responseHandler.onNewHandle(checkHandle(getHttpClient().get(getAbsoluteUrl(protocol, url.startsWith("servlet/") ? url : "servlet/" + url), null, new BinaryHttpResponseHandler(allowed) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                    Log.v(TAG, "getAvatar | url=" + url + " | success | statusCode=" + statusCode);
                    responseHandler.onNewHandle(null);
                    try {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.length);
                        responseHandler.onSuccess(statusCode, bitmap);
                    } catch (Exception e) {
                        Static.error(e);
                        responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable throwable) {
                    Log.v(TAG, "getAvatar | url=" + url + " | failure | statusCode=" + statusCode + (throwable != null ? " | throwable=" + throwable.getMessage() : "") + (binaryData != null ? " | response=" + convert2UTF8(headers, binaryData) : ""));
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(statusCode, FAILED_TRY_AGAIN);
                }
            })));
        } else {
            Log.v(TAG, "getAvatar | offline");
            responseHandler.onFailure(STATUS_CODE_EMPTY, FAILED_OFFLINE);
        }
    }

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
