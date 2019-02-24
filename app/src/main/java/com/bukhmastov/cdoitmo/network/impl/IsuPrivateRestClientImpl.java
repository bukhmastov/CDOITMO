package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.ResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestStringResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.handlers.joiner.RestResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.PropertiesUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    public void authorize(@NonNull Context context, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "authorize by refresh token");
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "authorize by refresh token | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            if (StringUtils.isBlank(refreshToken)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            Map<String, String> query = new HashMap<>();
            query.put("grant_type", "refresh_token");
            query.put("client_id", PropertiesUtils.getIsuProperty(context, "isu.api.client.id"));
            query.put("client_secret", PropertiesUtils.getIsuProperty(context, "isu.api.client.secret"));
            query.put("refresh_token", refreshToken);
            String url = getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken");
            doPost(context, url, query, new HashMap<>(), new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    log.v(TAG, "authorize by refresh token | success | code=", code);
                    if (code == 200) {
                        String accessToken = headers.get().get("access_token");
                        String expiresIn = headers.get().get("expires_in");
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
                            super.onFailure(code, headers, FAILED_AUTH);
                            return;
                        }
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                        String expiresAt;
                        try {
                            expiresAt = String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L);
                        } catch (Exception ignore) {
                            expiresAt = String.valueOf(time.getTimeInMillis() + TimeUnit.MINUTES.toMillis(30));
                        }
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", expiresAt);
                        super.onProgress(STATE_AUTHORIZED);
                        super.onSuccess(code, headers, "authorized");
                        return;
                    }
                    if (code == 401) {
                        super.onFailure(code, headers, FAILED_AUTH_CREDENTIALS_FAILED);
                        return;
                    }
                    super.onFailure(code, headers, FAILED_AUTH);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "authorize by refresh token | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull String username, @NonNull String password,
                          @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "authorize by password");
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "authorize by password | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            Map<String, String> query = new HashMap<>();
            query.put("grant_type", "password");
            query.put("client_id", PropertiesUtils.getIsuProperty(context, "isu.api.client.id"));
            query.put("username", username);
            query.put("password", password);
            String url = getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken");
            doPost(context, url, query, new HashMap<>(), new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    log.v(TAG, "authorize by password | success | code=", code);
                    if (code == 200) {
                        String accessToken = headers.get().get("access_token");
                        String expiresIn = headers.get().get("expires_in");
                        String refreshToken = headers.get().get("refresh_token");
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
                            super.onFailure(code, headers, FAILED_AUTH);
                            return;
                        }
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", accessToken);
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", refreshToken);
                        String expiresAt;
                        try {
                            expiresAt = String.valueOf((Long.parseLong(expiresIn)) * 1000L + time.getTimeInMillis() - 60000L);
                        } catch (Exception ignore) {
                            expiresAt = String.valueOf(time.getTimeInMillis() + TimeUnit.MINUTES.toMillis(30));
                        }
                        storage.put(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", expiresAt);
                        super.onProgress(STATE_AUTHORIZED);
                        super.onSuccess(code, headers, "authorized");
                        return;
                    }
                    if (code == 401) {
                        super.onFailure(code, headers, FAILED_AUTH_CREDENTIALS_FAILED);
                        return;
                    }
                    super.onFailure(code, headers, FAILED_AUTH);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "authorize by password | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    public <T extends JsonEntity> void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler);
    }

    @Override
    public <T extends JsonEntity> void get(@NonNull Context context,
                    @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        try {
            log.v(TAG, "get | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "get | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            checkIsAuthorized(context, new RestStringResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    if (!"authorized".equals(response)) {
                        log.v(TAG, "get | check failed | code=", code, " | response=", response);
                        handler.onFailure(code, headers, FAILED_AUTH_CREDENTIALS_REQUIRED);
                        return;
                    }
                    getAuthorized(context, protocol, url, query, handler);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    private void checkIsAuthorized(@NonNull Context context, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "check");
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "check | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            String accessToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#access_token", "").trim();
            String refreshToken = storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#refresh_token", "").trim();
            long expiresAt = Long.parseLong(storage.get(context, Storage.PERMANENT, Storage.USER, "user#isu#expires_at", "0").trim());
            if (expiresAt >= time.getTimeInMillis()) {
                if (StringUtils.isNotBlank(accessToken)) {
                    log.v(TAG, "check | all systems operational");
                    handler.onProgress(STATE_AUTHORIZED);
                    handler.onSuccess(STATUS_CODE_EMPTY, null, "authorized");
                } else {
                    log.v(TAG, "check | access token is empty");
                    handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_CREDENTIALS_REQUIRED);
                }
                return;
            }
            if (StringUtils.isBlank(refreshToken)) {
                log.v(TAG, "check | refresh token is empty");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            log.v(TAG, "check | refresh token expired, going to retrieve new access token");
            authorize(context, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    if ("authorized".equals(response)) {
                        log.v(TAG, "check | authorize | all systems operational");
                        handler.onProgress(STATE_AUTHORIZED);
                        handler.onSuccess(STATUS_CODE_EMPTY, null, "authorized");
                    } else {
                        log.v(TAG, "check | authorize | failed | code=", code, " | response=", response);
                        super.onFailure(code, headers, FAILED_AUTH);
                    }
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "check | authorize | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    private <T extends JsonEntity> void getAuthorized(@NonNull Context context,
                    @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull RestResponseHandler<T> handler) {
        log.v(TAG, "getAuthorized | url=", url);
        if (Client.isOffline(context)) {
            log.v(TAG, "getAuthorized | url=", url, " | offline");
            handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
            return;
        }
        handler.onProgress(STATE_HANDLING);
        doGetJson(context, getAbsoluteUrl(protocol, BASE_URL, url), query, new RestResponseHandlerJoiner<T>(handler) {
            @Override
            public void onSuccess(int code, Headers headers, T response) throws Exception {
                log.v(TAG, "getAuthorized | url=", url, " | success | code=", code);
                super.onSuccess(code, headers, response);
            }
            @Override
            public void onFailure(int code, Headers headers, int state) {
                log.v(TAG, "getAuthorized | url=", url, " | failed | code=", code, " | state=", state);
                super.onFailure(code, headers, state);
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
