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
    public void check(@NonNull Context context, @NonNull ResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "check");
            if (!Client.isOnline(context)) {
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            responseHandler.onProgress(STATE_CHECKING);
            String accessToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", "").trim();
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            long expiresAt = Long.parseLong(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", "0").trim());
            if (expiresAt >= time.getTimeInMillis()) {
                if (StringUtils.isNotBlank(accessToken)) {
                    log.v(TAG, "check | all systems operational");
                    responseHandler.onProgress(STATE_AUTHORIZED);
                    responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                } else {
                    log.v(TAG, "check | access token is empty");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                }
                return;
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                log.v(TAG, "check | refresh token is empty");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            log.v(TAG, "check | refresh token expired, going to retrieve new access token");
            authorize(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, String response) {
                    if ("authorized".equals(response)) {
                        log.v(TAG, "check | authorize | all systems operational");
                        responseHandler.onProgress(STATE_AUTHORIZED);
                        responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                    } else {
                        log.v(TAG, "check | authorize | failed | statusCode=", statusCode, " | response=", response);
                        responseHandler.onFailure(statusCode, headers, FAILED_AUTH_TRY_AGAIN);
                    }
                }
                @Override
                public void onFailure(int statusCode, Headers headers, int state) {
                    log.v(TAG, "check | authorize | failed | statusCode=", statusCode, " | state=", state);
                    responseHandler.onFailure(statusCode, headers, state);
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull ResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "authorize by refresh token");
            if (!Client.isOnline(context)) {
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            responseHandler.onProgress(STATE_AUTHORIZATION);
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            if (StringUtils.isBlank(refreshToken)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
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
                    thread.run(Thread.BACKGROUND, () -> {
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
                                        case "access_token": accessToken = qPair[1]; break;
                                        case "expires_in": expiresIn = qPair[1]; break;
                                    }
                                }
                            }
                            if (StringUtils.isBlank(accessToken)) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                return;
                            }
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                            try {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L));
                            } catch (Exception ignore) {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf(time.getTimeInMillis() + 1800000L)); // 30min
                            }
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(code, new Headers(headers), "authorized");
                            return;
                        }
                        if (code == 401) {
                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                            return;
                        }
                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    }, throwable -> {
                        log.v(TAG, "authorize by refresh token | success | exception | throwable=", throwable);
                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    thread.run(Thread.BACKGROUND, () -> {
                        log.v(TAG, "authorize by refresh token | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    });
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull String username, @NonNull String password, @NonNull ResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "authorize by password");
            if (!Client.isOnline(context)) {
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            responseHandler.onProgress(STATE_AUTHORIZATION);
            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
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
                    thread.run(Thread.BACKGROUND, () -> {
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
                                        case "access_token": accessToken = qPair[1]; break;
                                        case "expires_in": expiresIn = qPair[1]; break;
                                        case "refresh_token": refreshToken = qPair[1]; break;
                                    }
                                }
                            }
                            if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(refreshToken)) {
                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                return;
                            }
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                            storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", refreshToken);
                            try {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L));
                            } catch (Exception ignore) {
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", String.valueOf(time.getTimeInMillis() + 1800000L)); // 30min
                            }
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(code, new Headers(headers), "authorized");
                            return;
                        }
                        if (code == 401) {
                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                            return;
                        }
                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    }, throwable -> {
                        log.v(TAG, "authorize by password | success | exception | throwable=", throwable);
                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    thread.run(Thread.BACKGROUND, () -> {
                        log.v(TAG, "authorize by password | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                        responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_TRY_AGAIN);
                    });
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (!Client.isOnline(context)) {
                log.v(TAG, "get | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            check(context, new ResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Headers headers, String response) {
                    if (!"authorized".equals(response)) {
                        log.v(TAG, "get | check failed | statusCode=", statusCode, " | response=", response);
                        responseHandler.onFailure(statusCode, headers, FAILED_AUTH_CREDENTIALS_REQUIRED);
                        return;
                    }
                    getAuthorized(context, protocol, url, query, responseHandler);
                }
                @Override
                public void onFailure(int statusCode, Headers headers, int state) {
                    responseHandler.onFailure(statusCode, headers, state);
                }
                @Override
                public void onProgress(int state) {
                    responseHandler.onProgress(state);
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    private void getAuthorized(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url, @Nullable Map<String, String> query, @NonNull RestResponseHandler responseHandler) {
        thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "getAuthorized | url=", url);
            if (!Client.isOnline(context)) {
                log.v(TAG, "getAuthorized | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            responseHandler.onProgress(STATE_HANDLING);
            gJson(context, getAbsoluteUrl(protocol, BASE_URL, url), query, new RawJsonHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response, JSONObject obj, JSONArray arr) {
                    thread.run(Thread.BACKGROUND, () -> {
                        log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                        responseHandler.onSuccess(code, new Headers(headers), obj, arr);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    thread.run(Thread.BACKGROUND, () -> {
                        log.v(TAG, "get | url=", url, " | failure", (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                        responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                    });
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public boolean isAuthorized(@NonNull Context context) {
        return StringUtils.isNotBlank(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", "")) &&
                StringUtils.isNotBlank(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", ""));
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Client.Protocol String protocol, @NonNull String base, @NonNull String relativeUrl) {
        return getProtocol(protocol) + base + "/" + relativeUrl;
    }
}
