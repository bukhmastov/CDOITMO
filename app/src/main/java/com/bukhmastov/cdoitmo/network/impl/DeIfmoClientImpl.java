package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.parse.UserDataParse;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DeIfmoClientImpl extends DeIfmoClient {

    private static final String TAG = "DeIfmoClient";
    private static final String BASE_URL = "de.ifmo.ru";
    private static final String DEFAULT_PROTOCOL = HTTPS;
    private static final boolean DEFAULT_RE_AUTH = true;

    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void check(@NonNull final Context context, @NonNull final ResponseHandler responseHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "check");
            if (Client.isOnline(context)) {
                responseHandler.onProgress(STATE_CHECKING);
                if (App.UNAUTHORIZED_MODE) {
                    log.v(TAG, "check | UNAUTHORIZED_MODE | success");
                    responseHandler.onSuccess(200, new Headers(null), "");
                    return;
                }
                if (checkJsessionId(context)) {
                    authorize(context, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, String response) {
                            check(context, responseHandler);
                        }
                        @Override
                        public void onProgress(int state) {
                            responseHandler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, Headers headers, int state) {
                            responseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                } else {
                    get(context, "servlet/distributedCDE?Rule=editPersonProfile", null, new ResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Headers headers, final String response) {
                            log.v(TAG, "check | success | going to parse user data");
                            Thread.run(Thread.BACKGROUND, () -> new UserDataParse(response, result -> {
                                if (result != null) {
                                    log.v(TAG, "check | success | parsed");
                                    String name = com.bukhmastov.cdoitmo.util.TextUtils.getStringSafely(result, "name", "");
                                    String avatar = com.bukhmastov.cdoitmo.util.TextUtils.getStringSafely(result, "avatar", "");
                                    String group = com.bukhmastov.cdoitmo.util.TextUtils.getStringSafely(result, "group", "");
                                    String pref_group_force_override = storagePref.get(context, "pref_group_force_override", "");
                                    if (pref_group_force_override == null) {
                                        pref_group_force_override = "";
                                    } else {
                                        pref_group_force_override = pref_group_force_override.trim();
                                    }
                                    String[] groups = (pref_group_force_override.isEmpty() ? group : pref_group_force_override).split(",\\s|\\s|,");
                                    String g = storage.get(context, Storage.PERMANENT, Storage.USER, "user#group");
                                    boolean gFound = false;
                                    for (String g1 : groups) {
                                        if (g1.equals(g)) {
                                            gFound = true;
                                            break;
                                        }
                                    }
                                    if (!gFound) {
                                        g = groups.length > 0 ? groups[0] : "";
                                    }
                                    storage.put(context, Storage.PERMANENT, Storage.USER, "user#name", name);
                                    storage.put(context, Storage.PERMANENT, Storage.USER, "user#group", g);
                                    storage.put(context, Storage.PERMANENT, Storage.USER, "user#groups", TextUtils.join(", ", groups));
                                    storage.put(context, Storage.PERMANENT, Storage.USER, "user#avatar", avatar);
                                    try {
                                        storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "user#week", new JSONObject()
                                                .put("week", Integer.parseInt(result.getString("week")))
                                                .put("timestamp", Time.getCalendar().getTimeInMillis())
                                                .toString()
                                        );
                                    } catch (Exception e) {
                                        log.exception(e);
                                        storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "user#week");
                                    }
                                    firebaseAnalyticsProvider.setUserProperties(context, group);
                                    responseHandler.onSuccess(200, headers, "");
                                } else {
                                    log.v(TAG, "check | success | not parsed");
                                    responseHandler.onSuccess(200, headers, "");
                                }
                            }).run());
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onFailure(final int statusCode, final Headers headers, final int state) {
                            Thread.run(Thread.BACKGROUND, () -> {
                                log.v(TAG, "check | failed | statusCode=", statusCode, " | state=", state);
                                responseHandler.onFailure(statusCode, headers, state);
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                }
            } else {
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @Override
    public void authorize(@NonNull final Context context, @NonNull final ResponseHandler responseHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "authorize");
            responseHandler.onProgress(STATE_AUTHORIZATION);
            if (App.UNAUTHORIZED_MODE) {
                log.v(TAG, "authorize | UNAUTHORIZED_MODE | authorized");
                responseHandler.onProgress(STATE_AUTHORIZED);
                responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                return;
            }
            String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
            String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
            if (login.isEmpty() || password.isEmpty()) {
                log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put("Rule", "LOGON");
                params.put("LOGIN", login);
                params.put("PASSWD", password);
                p(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        Thread.run(Thread.BACKGROUND, new Runnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "authorize | success | code=", code);
                                storeCookies(context, headers);
                                if (code >= 400) {
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                    return;
                                }
                                try {
                                    if (response == null) {
                                        throw new NullPointerException("data cannot be null");
                                    }
                                    if (response.contains("Access is forbidden") && response.contains("Invalid login/password")) {
                                        log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                                    } else if (response.contains("Выбор группы безопасности") && response.contains("OPTION VALUE=8")) {
                                        log.v(TAG, "authorize | success | going to select security group");
                                        g(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login") + "&SECURITYGROUP=8&COMPNAME="), null, new RawHandler() {
                                            @Override
                                            public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                                                Thread.run(Thread.BACKGROUND, () -> {
                                                    if (code == 200) {
                                                        log.v(TAG, "authorize | success | security group | authorized | statusCode=" + code);
                                                        responseHandler.onProgress(STATE_AUTHORIZED);
                                                        responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                                    } else {
                                                        log.v(TAG, "authorize | success | security group | FAILED | statusCode=", code, response != null ? " | response=" + response : "");
                                                        responseHandler.onFailure(code, new Headers(headers), code >= 400 ? FAILED_SERVER_ERROR : FAILED_AUTH_TRY_AGAIN);
                                                    }
                                                });
                                            }
                                            @Override
                                            public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                                                Thread.run(Thread.BACKGROUND, () -> {
                                                    log.v(TAG, "authorize | success | security group | FAILED | statusCode=", code, " | throwable=", throwable);
                                                    responseHandler.onFailure(code, new Headers(headers), code >= 400 ? FAILED_SERVER_ERROR : FAILED_AUTH_TRY_AGAIN);
                                                });
                                            }
                                            @Override
                                            public void onNewRequest(Request request) {
                                                responseHandler.onNewRequest(request);
                                            }
                                        });
                                    } else if (response.contains("Обучение и аттестация")) {
                                        log.v(TAG, "authorize | success | authorized");
                                        responseHandler.onProgress(STATE_AUTHORIZED);
                                        responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                    } else {
                                        log.v(TAG, "authorize | success | FAILED_AUTH_TRY_AGAIN");
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                    }
                                } catch (Exception e) {
                                    log.exception(e);
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                }
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            log.v(TAG, "authorize | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), isInterrupted(throwable) ? FAILED_INTERRUPTED : (code >= 400 ? FAILED_SERVER_ERROR : FAILED_AUTH_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            }
        });
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler) {
        get(context, protocol, url, query, responseHandler, DEFAULT_RE_AUTH);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler, final boolean reAuth) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, reAuth);
    }

    @Override
    public void get(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final ResponseHandler responseHandler, final boolean reAuth) {
        Thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "get | url=", url);
            if (Client.isOnline(context)) {
                if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                    log.v(TAG, "get | UNAUTHORIZED_MODE | failed");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_UNAUTHORIZED_MODE);
                    return;
                }
                if (!App.UNAUTHORIZED_MODE && reAuth && checkJsessionId(context)) {
                    authorize(context, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, String response) {
                            get(context, protocol, url, query, responseHandler, true);
                        }
                        @Override
                        public void onProgress(int state) {
                            responseHandler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, Headers headers, int state) {
                            responseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                    return;
                }
                responseHandler.onProgress(STATE_HANDLING);
                g(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        Thread.run(Thread.BACKGROUND, new Runnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "get | url=", url, " | success | statusCode=", code);
                                try {
                                    if (code >= 400) {
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                        return;
                                    }
                                    if (response == null) {
                                        throw new NullPointerException("data cannot be null");
                                    }
                                    if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                                        log.v(TAG, "get | url=", url, " | success | auth required");
                                        if (!App.UNAUTHORIZED_MODE && reAuth) {
                                            authorize(context, new ResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Headers headers, String response) {
                                                    get(context, protocol, url, query, responseHandler, true);
                                                }
                                                @Override
                                                public void onProgress(int state) {
                                                    responseHandler.onProgress(state);
                                                }
                                                @Override
                                                public void onFailure(int statusCode, Headers headers, int state) {
                                                    responseHandler.onFailure(statusCode, headers, state);
                                                }
                                                @Override
                                                public void onNewRequest(Request request) {
                                                    responseHandler.onNewRequest(request);
                                                }
                                            });
                                        } else {
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_REQUIRED);
                                        }
                                    } else {
                                        responseHandler.onSuccess(code, new Headers(headers), response);
                                    }
                                } catch (Exception e) {
                                    log.exception(e);
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                                }
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            log.v(TAG, "get | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), isInterrupted(throwable) ? FAILED_INTERRUPTED : (code >= 400 ? FAILED_SERVER_ERROR : FAILED_AUTH_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                log.v(TAG, "get | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @Override
    public void post(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> params, @NonNull final ResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }

    @Override
    public void post(@NonNull final Context context, @NonNull final @Protocol String protocol, @NonNull final String url, @Nullable final Map<String, String> params, @NonNull final ResponseHandler responseHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            log.v(TAG, "post | url=", url);
            if (Client.isOnline(context)) {
                if (App.UNAUTHORIZED_MODE && !url.startsWith("index.php")) {
                    log.v(TAG, "post | UNAUTHORIZED_MODE | failed");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_UNAUTHORIZED_MODE);
                    return;
                }
                if (!App.UNAUTHORIZED_MODE && checkJsessionId(context)) {
                    authorize(context, new ResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Headers headers, String response) {
                            post(context, protocol, url, params, responseHandler);
                        }
                        @Override
                        public void onProgress(int state) {
                            responseHandler.onProgress(state);
                        }
                        @Override
                        public void onFailure(int statusCode, Headers headers, int state) {
                            responseHandler.onFailure(statusCode, headers, state);
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                    return;
                }
                responseHandler.onProgress(STATE_HANDLING);
                p(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        Thread.run(Thread.BACKGROUND, new Runnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "post | url=", url, " | success | statusCode=", code);
                                try {
                                    if (code >= 400) {
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                        return;
                                    }
                                    if (response == null)
                                        throw new NullPointerException("data cannot be null");
                                    if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                                        log.v(TAG, "post | url=", url, " | success | auth required");
                                        if (!App.UNAUTHORIZED_MODE) {
                                            authorize(context, new ResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Headers headers, String response) {
                                                    post(context, protocol, url, params, responseHandler);
                                                }
                                                @Override
                                                public void onProgress(int state) {
                                                    responseHandler.onProgress(state);
                                                }
                                                @Override
                                                public void onFailure(int statusCode, Headers headers, int state) {
                                                    responseHandler.onFailure(statusCode, headers, state);
                                                }
                                                @Override
                                                public void onNewRequest(Request request) {
                                                    responseHandler.onNewRequest(request);
                                                }
                                            });
                                        } else {
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_REQUIRED);
                                        }
                                    } else {
                                        responseHandler.onSuccess(code, new Headers(headers), response);
                                    }
                                } catch (Exception e) {
                                    log.exception(e);
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                                }
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            log.v(TAG, "post | url=", url, " | failure | statusCode=", code, " | throwable=", throwable);
                            responseHandler.onFailure(code, new Headers(headers), isInterrupted(throwable) ? FAILED_INTERRUPTED : (code >= 400 ? FAILED_SERVER_ERROR : FAILED_AUTH_TRY_AGAIN));
                        });
                    }
                    @Override
                    public void onNewRequest(Request request) {
                        responseHandler.onNewRequest(request);
                    }
                });
            } else {
                log.v(TAG, "p | offline");
                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
            }
        });
    }

    @Override
    public boolean isAuthorized(@NonNull final Context context) {
        final String login = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim();
        final String password = storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#password", "").trim();
        return !login.isEmpty() && !password.isEmpty();
    }

    @NonNull
    private String getAbsoluteUrl(@NonNull @Protocol String protocol, @NonNull String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
