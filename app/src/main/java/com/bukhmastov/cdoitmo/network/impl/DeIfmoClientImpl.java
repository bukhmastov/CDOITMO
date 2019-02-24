package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.ResponseHandlerJoiner;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
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
    Lazy<Storage> storage;

    public DeIfmoClientImpl() {
        super();
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void authorize(@NonNull Context context, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "authorize");
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            handler.onProgress(STATE_AUTHORIZATION);
            if (App.UNAUTHORIZED_MODE) {
                log.v(TAG, "authorize | UNAUTHORIZED_MODE | authorized");
                handler.onProgress(STATE_AUTHORIZED);
                handler.onSuccess(STATUS_CODE_EMPTY, null, "authorized");
                return;
            }
            String login = storage.get().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
            String password = storage.get().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
            if (StringUtils.isBlank(login) || StringUtils.isBlank(password)) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_CREDENTIALS_REQUIRED);
                return;
            }
            HashMap<String, String> params = new HashMap<>();
            params.put("Rule", "LOGON");
            params.put("LOGIN", login);
            params.put("PASSWD", password);
            doPost(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    log.v(TAG, "authorize | success | code=", code);
                    storeCookies(context, headers, true);
                    if (StringUtils.isBlank(response)) {
                        log.v(TAG, "authorize | success | code=", code, " | response is blank");
                        onFailure(code, headers, FAILED_EMPTY_RESPONSE);
                        return;
                    }
                    if (response.contains("Access is forbidden") && response.contains("Invalid login/password")) {
                        log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                        onFailure(code, headers, FAILED_AUTH_CREDENTIALS_FAILED);
                        return;
                    }
                    if (response.contains("Выбор группы безопасности") && response.contains("OPTION VALUE=8")) {
                        log.v(TAG, "authorize | success | going to select security group");
                        String url = "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" +
                                storage.get().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login") +
                                "&SECURITYGROUP=8&COMPNAME=";
                        doGet(context, getAbsoluteUrl(DEFAULT_PROTOCOL, url), null, new ResponseHandlerJoiner(handler) {
                            @Override
                            public void onSuccess(int code, Headers headers, String response) throws Exception {
                                if (code == 200) {
                                    log.v(TAG, "authorize | security group | success | authorized");
                                    handler.onProgress(STATE_AUTHORIZED);
                                    handler.onSuccess(code, headers, "authorized");
                                    return;
                                }
                                log.v(TAG, "authorize | security group | success | ",
                                        "not authorized | code=", code,
                                        response != null ? " | response=" + response : "");
                                onFailure(code, headers, FAILED_AUTH);
                            }
                            @Override
                            public void onFailure(int code, Headers headers, int state) {
                                log.v(TAG, "authorize | security group | failed | code=",
                                        code, " | state=", state);
                                super.onFailure(code, headers, state);
                            }
                        });
                        return;
                    }
                    if (response.contains("Обучение и аттестация")) {
                        log.v(TAG, "authorize | success | authorized");
                        handler.onProgress(STATE_AUTHORIZED);
                        handler.onSuccess(code, headers, "authorized");
                        return;
                    }
                    log.v(TAG, "authorize | success | FAILED_AUTH");
                    onFailure(code, headers, FAILED_AUTH);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "authorize | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        get(context, DEFAULT_PROTOCOL, url, query, handler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull Context context,
                    @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        get(context, protocol, url, query, handler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull Context context, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler, boolean reAuth) {
        get(context, DEFAULT_PROTOCOL, url, query, handler, reAuth);
    }

    @Override
    public void get(@NonNull Context context,
                    @NonNull @Protocol String protocol, @NonNull String url,
                    @Nullable Map<String, String> query, @NonNull ResponseHandler handler, boolean reAuth) {
        try {
            log.v(TAG, "get | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "get | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                log.v(TAG, "get | url=", url, " | denied | non-authorized mode tried to access non-index.php url");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_DENIED);
                return;
            }
            if (!App.UNAUTHORIZED_MODE && isAuthExpiredByJsessionId(context)) {
                if (!reAuth) {
                    log.v(TAG, "get | url=", url, " | auth expired and reAuth=false provided");
                    handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_AUTH_REQUIRED);
                    return;
                }
                authorize(context, new ResponseHandlerJoiner(handler) {
                    @Override
                    public void onSuccess(int code, Headers headers, String response) {
                        log.v(TAG, "get | url=", url, " | auth recovered");
                        get(context, protocol, url, query, handler, false);
                    }
                    @Override
                    public void onFailure(int code, Headers headers, int state) {
                        log.v(TAG, "get | url=", url, " | failed to recover auth");
                        super.onFailure(code, headers, FAILED_AUTH);
                    }
                });
                return;
            }
            handler.onProgress(STATE_HANDLING);
            doGet(context, getAbsoluteUrl(protocol, url), query, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    log.v(TAG, "get | url=", url, " | success | code=", code);
                    if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                        log.v(TAG, "get | url=", url, " | success | auth required");
                        if (App.UNAUTHORIZED_MODE || !reAuth) {
                            log.v(TAG, "get | url=", url, " | success | auth required | unauthorized mode or reAuth=false");
                            handler.onFailure(code, headers, FAILED_AUTH_REQUIRED);
                            return;
                        }
                        authorize(context, new ResponseHandlerJoiner(handler) {
                            @Override
                            public void onSuccess(int code, Headers headers, String response) {
                                log.v(TAG, "get | url=", url, " | success | auth recovered");
                                get(context, protocol, url, query, handler, false);
                            }
                            @Override
                            public void onFailure(int code, Headers headers, int state) {
                                log.v(TAG, "get | url=", url, " | success | failed to recover auth");
                                super.onFailure(code, headers, FAILED_AUTH);
                            }
                        });
                        return;
                    }
                    handler.onSuccess(code, headers, response);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    log.v(TAG, "get | url=", url, " | failed | code=", code, " | state=", state);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    public void post(@NonNull Context context, @NonNull String url,
                     @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        post(context, DEFAULT_PROTOCOL, url, params, handler);
    }

    @Override
    public void post(@NonNull Context context,
                     @NonNull @Protocol String protocol, @NonNull String url,
                     @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        try {
            log.v(TAG, "post | url=", url);
            thread.assertNotUI();
            if (Client.isOffline(context)) {
                log.v(TAG, "post | url=", url, " | offline");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_OFFLINE);
                return;
            }
            if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                log.v(TAG, "post | url=", url, " | denied | non-authorized mode tried to access non-index.php url");
                handler.onFailure(STATUS_CODE_EMPTY, null, FAILED_DENIED);
                return;
            }
            if (!App.UNAUTHORIZED_MODE && isAuthExpiredByJsessionId(context)) {
                authorize(context, new ResponseHandlerJoiner(handler) {
                    @Override
                    public void onSuccess(int code, Headers headers, String response) {
                        log.v(TAG, "post | url=", url, " | auth recovered");
                        post(context, protocol, url, params, handler);
                    }
                    @Override
                    public void onFailure(int code, Headers headers, int state) {
                        log.v(TAG, "post | url=", url, " | failed to recover auth");
                        super.onFailure(code, headers, FAILED_AUTH);
                    }
                });
                return;
            }
            handler.onProgress(STATE_HANDLING);
            doPost(context, getAbsoluteUrl(protocol, url), params, new ResponseHandlerJoiner(handler) {
            @Override
            public void onSuccess(int code, Headers headers, String response) throws Exception {
                log.v(TAG, "post | url=", url, " | success | code=", code);
                if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                    log.v(TAG, "post | url=", url, " | success | auth required");
                    if (App.UNAUTHORIZED_MODE) {
                        log.v(TAG, "post | url=", url, " | success | auth required | unauthorized mode");
                        handler.onFailure(code, headers, FAILED_AUTH_REQUIRED);
                        return;
                    }
                    authorize(context, new ResponseHandlerJoiner(handler) {
                        @Override
                        public void onSuccess(int code, Headers headers, String response) {
                            log.v(TAG, "post | url=", url, " | success | auth recovered");
                            post(context, protocol, url, params, handler);
                        }
                        @Override
                        public void onFailure(int code, Headers headers, int state) {
                            log.v(TAG, "post | url=", url, " | success | failed to recover auth");
                            super.onFailure(code, headers, FAILED_AUTH);
                        }
                    });
                    return;
                }
                handler.onSuccess(code, headers, response);
            }
            @Override
            public void onFailure(int code, Headers headers, int state) {
                log.v(TAG, "post | url=", url, " | failed | code=", code, " | state=", state);
                super.onFailure(code, headers, state);
            }
        });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    public boolean isAuthorized(@NonNull Context context) {
        thread.assertNotUI();
        String login = storage.get().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
        String password = storage.get().get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
        return StringUtils.isNotEmpty(login) && StringUtils.isNotEmpty(password);
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
