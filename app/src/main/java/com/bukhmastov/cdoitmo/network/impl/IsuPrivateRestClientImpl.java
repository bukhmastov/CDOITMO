package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.PropertiesUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IsuPrivateRestClientImpl extends IsuPrivateRestClient {

    private static final String TAG = "IsuPrivateRestClientImpl";
    private static final String BASE_URL = "isu.ifmo.ru/ords/isurest/v1/api/core";
    private static final String BASE_URL_AUTH = "services.ifmo.ru:8444/cas/oauth2.0";
    private static final String DEFAULT_PROTOCOL = HTTPS;
    private static final String DEFAULT_PROTOCOL_AUTH = HTTPS;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    Time time;

    public IsuPrivateRestClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void check(@NonNull Context context, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "check");
            thread.assertNotUI();
            if (!Client.isOnline(context)) {
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_CHECKING);
            String accessToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", "").trim();
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            long expiresAt = Long.parseLong(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", "0").trim());
            if (expiresAt >= time.getTimeInMillis()) {
                if (StringUtils.isNotBlank(accessToken)) {
                    log.v(TAG, "check | all systems operational");
                    handler.onProgress(STATE_AUTHORIZED);
                    handler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                } else {
                    log.v(TAG, "check | access token is empty");
                    handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                }
                return;
            }
            if (StringUtils.isBlank(refreshToken)) {
                log.v(TAG, "check | refresh token is empty");
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            log.v(TAG, "check | refresh token expired, going to retrieve new access token");
            authorize(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, String response) throws Exception {
                    if ("authorized".equals(response)) {
                        log.v(TAG, "check | authorize | all systems operational");
                        handler.onProgress(STATE_AUTHORIZED);
                        handler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                    } else {
                        log.v(TAG, "check | authorize | failed | statusCode=", statusCode, " | response=", response);
                        handler.onFailure(statusCode, headers, FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Headers headers, int state) {
                    log.v(TAG, "check | authorize | failed | statusCode=", statusCode, " | state=", state);
                    handler.onFailure(statusCode, headers, state);
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Request request) {
                    handler.onNewRequest(request);
                }
            });
        } catch (Throwable throwable) {
            invokeOnFailed(handler, STATUS_CODE_EMPTY, null, throwable, FAILED_TRY_AGAIN);
        }
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "authorize by refresh token");
            thread.assertNotUI();
            if (!Client.isOnline(context)) {
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            if (StringUtils.isBlank(refreshToken)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            Map<String, String> query = new HashMap<>();
            query.put("grant_type", "refresh_token");
            query.put("client_id", PropertiesUtils.getIsuProperty(context, "isu.api.client.id"));
            query.put("client_secret", PropertiesUtils.getIsuProperty(context, "isu.api.client.secret"));
            query.put("refresh_token", refreshToken);
            doPost(context, getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken"), query, new HashMap<>(), new RawHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response) {
                    try {
                        log.v(TAG, "authorize by refresh token | success | code=", code);
                        if (code == 200) {
                            String accessToken = headers.get("access_token");
                            String expiresIn = headers.get("expires_in");
                            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(expiresIn)) {
                                for (String qParam : response.split("&")) {
                                    String[] qPair = qParam.split("=");
                                    if (qPair.length != 2) {
                                        continue;
                                    }
                                    switch (qPair[0]) {
                                        case "access_token":
                                            accessToken = qPair[1];
                                            break;
                                        case "expires_in":
                                            expiresIn = qPair[1];
                                            break;
                                    }
                                }
                            }
                            if (StringUtils.isBlank(accessToken)) {
                                handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                return;
                            }
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                            try {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L));
                            } catch (Exception ignore) {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf(time.getTimeInMillis() + 1800000L)); // 30min
                            }
                            handler.onProgress(STATE_AUTHORIZED);
                            handler.onSuccess(code, new Headers(headers), "authorized");
                            return;
                        }
                        if (code == 401) {
                            handler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                            return;
                        }
                        handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    } catch (Throwable throwable) {
                        log.v(TAG, "authorize by refresh token | success | exception | throwable=", throwable);
                        handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    log.v(TAG, "authorize by refresh token | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                    invokeOnFailed(handler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
                }
                @Override
                public void onNewRequest(Request request) {
                    handler.onNewRequest(request);
                }
            });
        } catch (Throwable throwable) {
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_TRY_AGAIN);
        }
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull String username, @NonNull String password,
                          @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "authorize by password");
            thread.assertNotUI();
            if (!Client.isOnline(context)) {
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            Map<String, String> query = new HashMap<>();
            query.put("grant_type", "password");
            query.put("client_id", PropertiesUtils.getIsuProperty(context, "isu.api.client.id"));
            query.put("username", username);
            query.put("password", password);
            doPost(context, getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken"), query, new HashMap<>(), new RawHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response) {
                    try {
                        log.v(TAG, "authorize by password | success | code=", code);
                        if (code == 200) {
                            String accessToken = headers.get("access_token");
                            String expiresIn = headers.get("expires_in");
                            String refreshToken = headers.get("refresh_token");
                            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(expiresIn) || StringUtils.isBlank(refreshToken)) {
                                for (String qParam : response.split("&")) {
                                    String[] qPair = qParam.split("=");
                                    if (qPair.length != 2) {
                                        continue;
                                    }
                                    switch (qPair[0]) {
                                        case "access_token":
                                            accessToken = qPair[1];
                                            break;
                                        case "expires_in":
                                            expiresIn = qPair[1];
                                            break;
                                        case "refresh_token":
                                            refreshToken = qPair[1];
                                            break;
                                    }
                                }
                            }
                            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
                                handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                return;
                            }
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", refreshToken);
                            try {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L));
                            } catch (Exception ignore) {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf(time.getTimeInMillis() + 1800000L)); // 30min
                            }
                            handler.onProgress(STATE_AUTHORIZED);
                            handler.onSuccess(code, new Headers(headers), "authorized");
                            return;
                        }
                        if (code == 401) {
                            handler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                            return;
                        }
                        handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    } catch (Throwable throwable) {
                        log.v(TAG, "authorize by password | success | exception | throwable=", throwable);
                        handler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    log.v(TAG, "authorize by password | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                    invokeOnFailed(handler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
                }
                @Override
                public void onNewRequest(Request request) {
                    handler.onNewRequest(request);
                }
            });
        } catch (Throwable throwable) {
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_TRY_AGAIN);
        }
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Protocol String protocol,
                    @NonNull String url, @Nullable Map<String, String> query,
                    @NonNull RestResponseHandler handler) {
        log.v(TAG, "get | url=", url);
        thread.assertNotUI();
        if (!Client.isOnline(context)) {
            log.v(TAG, "get | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            return;
        }
        check(context, new ResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, String response) {
                if (!"authorized".equals(response)) {
                    log.v(TAG, "get | check failed | statusCode=", statusCode, " | response=", response);
                    handler.onFailure(statusCode, headers, FAILED_AUTH_CREDENTIALS_REQUIRED);
                    return;
                }
                getAuthorized(context, protocol, url, query, handler);
            }
            @Override
            public void onFailure(int statusCode, Headers headers, int state) {
                handler.onFailure(statusCode, headers, state);
            }
            @Override
            public void onProgress(int state) {
                handler.onProgress(state);
            }
            @Override
            public void onNewRequest(Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    private void getAuthorized(@NonNull Context context, @NonNull @Protocol String protocol,
                               @NonNull String url, @Nullable Map<String, String> query,
                               @NonNull RestResponseHandler handler) {
        log.v(TAG, "getAuthorized | url=", url);
        if (!Client.isOnline(context)) {
            log.v(TAG, "getAuthorized | offline");
            handler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            return;
        }
        handler.onProgress(STATE_HANDLING);
        gJson(context, getAbsoluteUrl(protocol, BASE_URL, url), query, new RawJsonHandler() {
            @Override
            public void onDone(int code, okhttp3.Headers headers, String response, JSONObject obj, JSONArray arr) throws Exception {
                log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                handler.onSuccess(code, new Headers(headers), obj, arr);
            }
            @Override
            public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                log.v(TAG, "get | url=", url, " | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                invokeOnFailed(handler, code, headers, throwable, FAILED_TRY_AGAIN);
            }
            @Override
            public void onNewRequest(Request request) {
                handler.onNewRequest(request);
            }
        });
    }

    @Override
    public boolean isAuthorized(@NonNull Context context) {
        thread.assertNotUI();
        return StringUtils.isNotBlank(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", "")) &&
                StringUtils.isNotBlank(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", ""));
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Client.Protocol String protocol, @NonNull String base, @NonNull String relativeUrl) {
        return getProtocol(protocol) + base + "/" + relativeUrl;
    }
}
