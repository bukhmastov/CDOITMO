package com.bukhmastov.cdoitmo.network.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;
import com.bukhmastov.cdoitmo.model.parser.UserDataParser;
import com.bukhmastov.cdoitmo.model.user.UserData;
import com.bukhmastov.cdoitmo.model.user.UserWeek;
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
    public void check(@NonNull final Context context, @NonNull final ResponseHandler responseHandler) {
        thread.run(thread.BACKGROUND, () -> {
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
                            thread.run(Thread.BACKGROUND, () -> {
                                UserData userData = new UserDataParser(response).parse();
                                if (userData == null) {
                                    log.v(TAG, "check | success | not parsed");
                                    responseHandler.onSuccess(200, headers, "");
                                    return;
                                }
                                log.v(TAG, "check | success | parsed");
                                String overrideGroup = storagePref.get(context, "pref_group_force_override", "");
                                String[] groups = (StringUtils.isBlank(overrideGroup) ? userData.getGroup() : overrideGroup.trim()).split(",\\s|\\s|,");
                                String group = storage.get(context, Storage.PERMANENT, Storage.USER, "user#group");
                                boolean gFound = false;
                                for (String g1 : groups) {
                                    if (g1.equals(group)) {
                                        gFound = true;
                                        break;
                                    }
                                }
                                if (!gFound) {
                                    group = groups.length > 0 ? groups[0] : "";
                                }
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#name", userData.getName());
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#group", group);
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#groups", android.text.TextUtils.join(", ", groups));
                                storage.put(context, Storage.PERMANENT, Storage.USER, "user#avatar", userData.getAvatar());
                                try {
                                    storage.put(context, Storage.PERMANENT, Storage.GLOBAL, "user#week", new UserWeek(
                                            userData.getWeek(),
                                            time.get().getTimeInMillis()
                                    ).toJsonString());
                                } catch (Exception e) {
                                    log.exception(e);
                                    storage.delete(context, Storage.PERMANENT, Storage.GLOBAL, "user#week");
                                }
                                firebaseAnalyticsProvider.get().setUserProperties(context, group);
                                responseHandler.onSuccess(200, headers, "");
                            });
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onFailure(final int statusCode, final Headers headers, final int state) {
                            thread.run(thread.BACKGROUND, () -> {
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
        thread.run(thread.BACKGROUND, () -> {
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
                doPost(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        thread.run(thread.BACKGROUND, new ThrowingRunnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "authorize | success | code=", code);
                                storeCookies(context, headers);
                                if (code >= 400) {
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                    return;
                                }
                                if (response == null) {
                                    throw new NullPointerException("data cannot be null");
                                }
                                if (response.contains("Access is forbidden") && response.contains("Invalid login/password")) {
                                    log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                                    responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                                } else if (response.contains("Выбор группы безопасности") && response.contains("OPTION VALUE=8")) {
                                    log.v(TAG, "authorize | success | going to select security group");
                                    doGet(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + storage.get(context, Storage.PERMANENT, Storage.USER, "user#deifmo#login") + "&SECURITYGROUP=8&COMPNAME="), null, new RawHandler() {
                                        @Override
                                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                                            thread.run(thread.BACKGROUND, () -> {
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
                                            thread.run(thread.BACKGROUND, () -> {
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
                            }
                        }, new ThrowingConsumer<Throwable, Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                log.exception(throwable);
                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        thread.run(thread.BACKGROUND, () -> {
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
        thread.run(thread.BACKGROUND, () -> {
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
                doGet(context, getAbsoluteUrl(protocol, url), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        thread.run(thread.BACKGROUND, new ThrowingRunnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "get | url=", url, " | success | statusCode=", code);
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
                            }
                        }, new ThrowingConsumer<Throwable, Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                log.exception(throwable);
                                responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        thread.run(thread.BACKGROUND, () -> {
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
        thread.run(thread.BACKGROUND, () -> {
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
                doPost(context, getAbsoluteUrl(protocol, url), params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        //noinspection Convert2Lambda
                        thread.run(thread.BACKGROUND, new ThrowingRunnable() {
                            @Override
                            public void run() {
                                log.v(TAG, "post | url=", url, " | success | statusCode=", code);
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
                            }
                        }, new ThrowingConsumer<Throwable, Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Throwable {
                                log.exception(throwable);
                                responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                            }
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        thread.run(thread.BACKGROUND, () -> {
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
