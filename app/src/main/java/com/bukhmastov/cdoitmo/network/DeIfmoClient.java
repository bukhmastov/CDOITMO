package com.bukhmastov.cdoitmo.network;

import android.content.Context;
import android.util.Log;

import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoClientResponseHandler;
import com.bukhmastov.cdoitmo.parse.UserDataParse;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class DeIfmoClient extends Client {

    private static final String TAG = "DeIfmoClient";
    private static final String BASE_URL = "http://de.ifmo.ru/";

    public static final int STATE_CHECKING = 0;
    public static final int STATE_AUTHORIZATION = 1;
    public static final int STATE_AUTHORIZED = 2;
    public static final int STATE_HANDLING = 3;
    public static final int FAILED_OFFLINE = 0;
    public static final int FAILED_TRY_AGAIN = 1;
    public static final int FAILED_AUTH_TRY_AGAIN = 2;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 3;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 4;

    public static void check(final Context context, final DeIfmoClientResponseHandler responseHandler){
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
                    public void onFailure(int state) {
                        responseHandler.onFailure(state);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            } else {
                DeIfmoClient.get(context, "servlet/distributedCDE?Rule=editPersonProfile", null, new DeIfmoClientResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, final String response) {
                        new UserDataParse(new UserDataParse.response() {
                            @Override
                            public void finish(HashMap<String, String> result) {
                                if (result != null) {
                                    Storage.file.perm.put(context, "user#name", result.get("name"));
                                    Storage.file.perm.put(context, "user#group", result.get("group"));
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("week", Integer.parseInt(result.get("week")));
                                        Storage.file.general.put(context, "user#week", jsonObject.toString());
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Storage.file.general.delete(context, "user#week");
                                    }
                                    responseHandler.onSuccess(200, "");
                                } else {
                                    responseHandler.onSuccess(200, "");
                                }
                            }
                        }).execute(response);
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onFailure(int state) {
                        responseHandler.onFailure(FAILED_TRY_AGAIN);
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {
                        responseHandler.onNewHandle(requestHandle);
                    }
                });
            }
        } else {
            responseHandler.onFailure(FAILED_OFFLINE);
        }
    }
    public static void authorize(final Context context, final DeIfmoClientResponseHandler responseHandler){
        init();
        responseHandler.onProgress(STATE_AUTHORIZATION);
        String login = Storage.file.perm.get(context, "user#login");
        String password = Storage.file.perm.get(context, "user#password");
        if (Objects.equals(login, "") || Objects.equals(password, "")) {
            responseHandler.onFailure(FAILED_AUTH_CREDENTIALS_REQUIRED);
        } else {
            RequestParams params = new RequestParams();
            params.put("Rule", "LOGON");
            params.put("LOGIN", login);
            params.put("PASSWD", password);
            renewCookie(context);
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl("servlet"), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    for (Header header : headers) {
                        if (Objects.equals(header.getName(), "Set-Cookie")) {
                            String[] pairs = header.getValue().split(";");
                            for (String pair : pairs) {
                                String[] cookie = pair.split("=");
                                if (Objects.equals(cookie[0], "JSESSIONID")) {
                                    if (!Objects.equals(cookie[1], "") && cookie[1] != null) {
                                        Storage.file.perm.put(context, "user#jsessionid", cookie[1]);
                                    } else {
                                        Log.w(TAG, "Got 'Set-Cookie' with empty 'JSESSIONID'");
                                        responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    try {
                        String data = new String((new String(responseBody, "windows-1251")).getBytes("UTF-8"));
                        if (data.contains("Access is forbidden") && data.contains("Invalid login/password")) {
                            responseHandler.onFailure(FAILED_AUTH_CREDENTIALS_FAILED);
                        } else if (data.contains("Выбор группы безопасности") && data.contains("OPTION VALUE=8")) {
                            httpclient.get(getAbsoluteUrl("servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + Storage.file.perm.get(context, "user#login") + "&SECURITYGROUP=8&COMPNAME="), null, new AsyncHttpResponseHandler(){
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                                    responseHandler.onProgress(STATE_AUTHORIZED);
                                    responseHandler.onSuccess(statusCode, "authorized");
                                }
                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                                    responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                                }
                            });
                        } else if (data.contains("Обучение и аттестация")) {
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(statusCode, "authorized");
                        } else {
                            responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                        }
                    } catch (UnsupportedEncodingException e) {
                        Static.error(e);
                        responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onNewHandle(null);
                    responseHandler.onFailure(FAILED_AUTH_TRY_AGAIN);
                }
            }));
        }
    }
    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler){
        get(context, url, params, responseHandler, true);
    }
    public static void get(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler, final boolean reAuth){
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
            responseHandler.onNewHandle(httpclient.get(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    try {
                        if(responseBody == null) throw new NullPointerException("responseBody cannot be null");
                        String data;
                        String charset = "windows-1251";
                        Matcher m = Pattern.compile("<meta.*charset=\"?(.*)\".*>").matcher(new String(responseBody, "UTF-8"));
                        if (m.find()) charset = m.group(1).toUpperCase();
                        if (Objects.equals(charset, "UTF-8")) {
                            data = new String(responseBody, charset);
                        } else {
                            data = new String((new String(responseBody, charset)).getBytes("UTF-8"));
                        }
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
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
                                    public void onFailure(int state) {
                                        responseHandler.onFailure(state);
                                    }
                                    @Override
                                    public void onNewHandle(RequestHandle requestHandle) {
                                        responseHandler.onNewHandle(requestHandle);
                                    }
                                });
                            } else {
                                responseHandler.onFailure(FAILED_AUTH_CREDENTIALS_REQUIRED);
                            }
                        } else {
                            responseHandler.onSuccess(statusCode, data);
                        }
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
    public static void post(final Context context, final String url, final RequestParams params, final DeIfmoClientResponseHandler responseHandler){
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
            responseHandler.onNewHandle(httpclient.post(getAbsoluteUrl(url), params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onNewHandle(null);
                    try {
                        if(responseBody == null) throw new NullPointerException("responseBody cannot be null");
                        String data;
                        String charset = "windows-1251";
                        Matcher m = Pattern.compile("<meta.*charset=\"?(.*)\".*>").matcher(new String(responseBody, "UTF-8"));
                        if (m.find()) charset = m.group(1).toUpperCase();
                        if (Objects.equals(charset, "UTF-8")) {
                            data = new String(responseBody, charset);
                        } else {
                            data = new String((new String(responseBody, charset)).getBytes("UTF-8"));
                        }
                        if (data.contains("Закончился интервал неактивности") || data.contains("Доступ запрещен")) {
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
                                public void onFailure(int state) {
                                    responseHandler.onFailure(state);
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