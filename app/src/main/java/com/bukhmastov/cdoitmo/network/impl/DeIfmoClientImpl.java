package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.Lazy;

public class DeIfmoClientImpl extends DeIfmoClient {

    private static final String TAG = "DeIfmoClient";
    private static final String BASE_URL = "de.ifmo.ru";
    private static final String DEFAULT_PROTOCOL = HTTPS;
    private static final boolean DEFAULT_RE_AUTH = true;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    Lazy<Time> time;
    @Inject
    Lazy<TextUtils> textUtils;
    @Inject
    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;

    public DeIfmoClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull ResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "authorize");
            if (!Client.isOnline(context)) {
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            responseHandler.onProgress(STATE_AUTHORIZATION);
            if (App.UNAUTHORIZED_MODE) {
                log.v(TAG, "authorize | UNAUTHORIZED_MODE | authorized");
                responseHandler.onProgress(STATE_AUTHORIZED);
                responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                return;
            }
            String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
            String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
            if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            HashMap<String, String> params = new HashMap<>();
            params.put("Rule", "LOGON");
            params.put("LOGIN", login);
            params.put("PASSWD", password);
            doPost(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new RawHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response) {
                    thread.run(thread.BACKGROUND, () -> {
                        log.v(TAG, "authorize | success | code=", code);
                        storeCookies(context, headers);
                        if (code >= 400) {
                            responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                            return;
                        }
                        if (StringUtils.isBlank(response)) {
                            log.v(TAG, "authorize | success | code=", code, " | response is blank");
                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                            return;
                        }
                        if (response.contains("Access is forbidden") && response.contains("Invalid login/password")) {
                            log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                            return;
                        }
                        if (response.contains("Выбор группы безопасности") && response.contains("OPTION VALUE=8")) {
                            log.v(TAG, "authorize | success | going to select security group");
                            doGet(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login") + "&SECURITYGROUP=8&COMPNAME="), null, new RawHandler() {
                                @Override
                                public void onDone(int code, okhttp3.Headers headers, String response) {
                                    if (code == 200) {
                                        log.v(TAG, "authorize | success | security group | authorized | code=" + code);
                                        responseHandler.onProgress(STATE_AUTHORIZED);
                                        responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                        return;
                                    }
                                    log.v(TAG, "authorize | success | security group | FAILED | code=", code, response != null ? " | response=" + response : "");
                                    invokeOnFailed(responseHandler, code, headers, FAILED_AUTH_TRY_AGAIN);
                                }
                                @Override
                                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                                    log.v(TAG, "authorize | success | security group | FAILED | code=", code, " | throwable=", throwable);
                                    invokeOnFailed(responseHandler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
                                }
                                @Override
                                public void onNewRequest(Request request) {
                                    responseHandler.onNewRequest(request);
                                }
                            });
                            return;
                        }
                        if (response.contains("Обучение и аттестация")) {
                            log.v(TAG, "authorize | success | authorized");
                            responseHandler.onProgress(STATE_AUTHORIZED);
                            responseHandler.onSuccess(code, new Headers(headers), "authorized");
                            return;
                        }
                        log.v(TAG, "authorize | success | FAILED_AUTH_TRY_AGAIN");
                        invokeOnFailed(responseHandler, code, headers, FAILED_AUTH_TRY_AGAIN);
                    }, throwable -> {
                        log.exception(throwable);
                        invokeOnFailed(responseHandler, code, headers, FAILED_AUTH_TRY_AGAIN);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    log.v(TAG, "authorize | failure | code=", code, " | throwable=", throwable);
                    invokeOnFailed(responseHandler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query,
                    @NonNull ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler) {
        get(context, protocol, url, query, responseHandler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> query,
                    @NonNull ResponseHandler responseHandler, boolean reAuth) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, reAuth);
    }

    @Override
    public void get(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler responseHandler, boolean reAuth) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (!Client.isOnline(context)) {
                log.v(TAG, "get | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                log.v(TAG, "get | UNAUTHORIZED_MODE | failed");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_UNAUTHORIZED_MODE);
                return;
            }
            if (!App.UNAUTHORIZED_MODE && isAuthExpiredByJsessionId(context)) {
                if (!reAuth) {
                    log.v(TAG, "get | auth expired and reAuth=false provided");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                    return;
                }
                authorize(context, new ResponseHandler() {
                    @Override
                    public void onSuccess(int code, Headers headers, String response) {
                        get(context, protocol, url, query, responseHandler, false);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int code, Headers headers, int state) {
                        responseHandler.onFailure(code, headers, state);
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
                return;
            }
            responseHandler.onProgress(STATE_HANDLING);
            doGet(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response) {
                    thread.run(thread.BACKGROUND, () -> {
                        log.v(TAG, "get | url=", url, " | success | code=", code);
                        if (code >= 400) {
                            responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                            return;
                        }
                        if (response == null) {
                            log.v(TAG, "get | url=", url, " | success | code=", code, " | response is null");
                            responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                            return;
                        }
                        if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                            log.v(TAG, "get | url=", url, " | success | auth required");
                            if (App.UNAUTHORIZED_MODE || !reAuth) {
                                log.v(TAG, "get | url=", url, " | success | auth required | unauthorized mode or reAuth=false");
                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_REQUIRED);
                                return;
                            }
                            authorize(context, new ResponseHandler() {
                                @Override
                                public void onSuccess(int code1, Headers headers1, String response1) {
                                    get(context, protocol, url, query, responseHandler, false);
                                }
                                @Override
                                public void onProgress(int state) {
                                    responseHandler.onProgress(state);
                                }
                                @Override
                                public void onFailure(int code1, Headers headers1, int state) {
                                    responseHandler.onFailure(code1, headers1, state);
                                }
                                @Override
                                public void onNewRequest(Request request) {
                                    responseHandler.onNewRequest(request);
                                }
                            });
                            return;
                        }
                        responseHandler.onSuccess(code, new Headers(headers), response);
                    }, throwable -> {
                        log.exception(throwable);
                        invokeOnFailed(responseHandler, code, headers, throwable, FAILED_TRY_AGAIN);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    log.v(TAG, "get | url=", url, " | failure | code=", code, " | throwable=", throwable);
                    invokeOnFailed(responseHandler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
                }
                @Override
                public void onNewRequest(Request request) {
                    responseHandler.onNewRequest(request);
                }
            });
        });
    }

    @Override
    public void post(@NonNull Context context, @NonNull String url, @Nullable Map<String, String> params,
                     @NonNull ResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }

    @Override
    public void post(@NonNull Context context, @NonNull @Protocol String protocol, @NonNull String url,
                     @Nullable Map<String, String> params, @NonNull ResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
            log.v(TAG, "post | url=", url);
            if (!Client.isOnline(context)) {
                log.v(TAG, "post | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                return;
            }
            if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                log.v(TAG, "post | UNAUTHORIZED_MODE | failed");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_UNAUTHORIZED_MODE);
                return;
            }
            if (!App.UNAUTHORIZED_MODE && isAuthExpiredByJsessionId(context)) {
                authorize(context, new ResponseHandler() {
                    @Override
                    public void onSuccess(int code, Headers headers, String response) {
                        post(context, protocol, url, params, responseHandler);
                    }
                    @Override
                    public void onProgress(int state) {
                        responseHandler.onProgress(state);
                    }
                    @Override
                    public void onFailure(int code, Headers headers, int state) {
                        responseHandler.onFailure(code, headers, state);
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
                return;
            }
            responseHandler.onProgress(STATE_HANDLING);
            doPost(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
                @Override
                public void onDone(int code, okhttp3.Headers headers, String response) {
                    thread.run(thread.BACKGROUND, () -> {
                        log.v(TAG, "post | url=", url, " | success | code=", code);
                        if (code >= 400) {
                            responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                            return;
                        }
                        if (response == null) {
                            log.v(TAG, "post | url=", url, " | success | code=", code, " | response is null");
                            responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                            return;
                        }
                        if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                            log.v(TAG, "post | url=", url, " | success | auth required");
                            if (App.UNAUTHORIZED_MODE) {
                                log.v(TAG, "post | url=", url, " | success | auth required | unauthorized mode");
                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_REQUIRED);
                                return;
                            }
                            authorize(context, new ResponseHandler() {
                                @Override
                                public void onSuccess(int code, Headers headers, String response1) {
                                    post(context, protocol, url, params, responseHandler);
                                }
                                @Override
                                public void onProgress(int state) {
                                    responseHandler.onProgress(state);
                                }
                                @Override
                                public void onFailure(int code, Headers headers, int state) {
                                    responseHandler.onFailure(code, headers, state);
                                }
                                @Override
                                public void onNewRequest(Request request) {
                                    responseHandler.onNewRequest(request);
                                }
                            });
                            return;
                        }
                        responseHandler.onSuccess(code, new Headers(headers), response);
                    }, throwable -> {
                        log.exception(throwable);
                        invokeOnFailed(responseHandler, code, headers, throwable, FAILED_TRY_AGAIN);
                    });
                }
                @Override
                public void onError(int code, okhttp3.Headers headers, Throwable throwable) {
                    log.v(TAG, "post | url=", url, " | failure | code=", code, " | throwable=", throwable);
                    invokeOnFailed(responseHandler, code, headers, throwable, FAILED_AUTH_TRY_AGAIN);
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
        String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
        String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
        return StringUtils.isNotEmpty(login) && StringUtils.isNotEmpty(password);
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
