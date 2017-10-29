package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.models.DeIfmo;
import com.bukhmastov.cdoitmo.parse.UserDataParse;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DeIfmoClient extends DeIfmo {

    private static final String TAG = "DeIfmoClient";
    private static final String BASE_URL = "de.ifmo.ru";
    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTPS;
    private static final boolean DEFAULT_RE_AUTH = true;

    public static void check(final Context context, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "check");
                if (Static.isOnline(context)) {
                    responseHandler.onProgress(STATE_CHECKING);
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
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "check | success | going to parse user data");
                                        new UserDataParse(response, new UserDataParse.response() {
                                            @Override
                                            public void finish(final HashMap<String, String> result) {
                                                if (result != null) {
                                                    Log.v(TAG, "check | success | parsed");
                                                    String pref_group_force_override = Storage.pref.get(context, "pref_group_force_override", "");
                                                    if (pref_group_force_override == null) {
                                                        pref_group_force_override = "";
                                                    } else {
                                                        pref_group_force_override = pref_group_force_override.trim();
                                                    }
                                                    Storage.file.perm.put(context, "user#name", result.get("name"));
                                                    Storage.file.perm.put(context, "user#group", pref_group_force_override.isEmpty() ? result.get("group") : pref_group_force_override);
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
                                                    responseHandler.onSuccess(200, headers, "");
                                                } else {
                                                    Log.v(TAG, "check | success | not parsed");
                                                    responseHandler.onSuccess(200, headers, "");
                                                }
                                            }
                                        }).run();
                                    }
                                });
                            }
                            @Override
                            public void onProgress(int state) {}
                            @Override
                            public void onFailure(final int statusCode, final Headers headers, final int state) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "check | failed | statusCode=" + statusCode + " | state=" + state);
                                        responseHandler.onFailure(statusCode, headers, state);
                                    }
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
            }
        });
    }
    public static void authorize(final Context context, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "authorize");
                responseHandler.onProgress(STATE_AUTHORIZATION);
                String login = Storage.file.perm.get(context, "user#deifmo#login", "").trim();
                String password = Storage.file.perm.get(context, "user#deifmo#password", "").trim();
                if (login.isEmpty() || password.isEmpty()) {
                    Log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                } else {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("Rule", "LOGON");
                    params.put("LOGIN", login);
                    params.put("PASSWD", password);
                    p(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet"), params, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "authorize | success | code=" + code);
                                    storeCookies(context, headers);
                                    if (code >= 500 && code < 600) {
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                        return;
                                    }
                                    try {
                                        if (response == null) throw new NullPointerException("data cannot be null");
                                        if (response.contains("Access is forbidden") && response.contains("Invalid login/password")) {
                                            Log.v(TAG, "authorize | success | FAILED_AUTH_CREDENTIALS_FAILED");
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                                        } else if (response.contains("Выбор группы безопасности") && response.contains("OPTION VALUE=8")) {
                                            Log.v(TAG, "authorize | success | going to select security group");
                                            g(context, getAbsoluteUrl(DEFAULT_PROTOCOL, "servlet/distributedCDE?Rule=APPLYSECURITYGROUP&PERSON=" + Storage.file.perm.get(context, "user#deifmo#login") + "&SECURITYGROUP=8&COMPNAME="), null, new RawHandler() {
                                                @Override
                                                public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                                                    Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (code == 200) {
                                                                Log.v(TAG, "authorize | success | security group | authorized | statusCode=" + code);
                                                                responseHandler.onProgress(STATE_AUTHORIZED);
                                                                responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                                            } else {
                                                                Log.v(TAG, "authorize | success | security group | FAILED_AUTH_TRY_AGAIN | statusCode=" + code + (response != null ? " | response=" + response : ""));
                                                                responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                                            }
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onError(final Throwable throwable) {
                                                    Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Log.v(TAG, "authorize | success | security group | FAILED_AUTH_TRY_AGAIN" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                                            responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_TRY_AGAIN);
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onNewRequest(Request request) {
                                                    responseHandler.onNewRequest(request);
                                                }
                                            });
                                        } else if (response.contains("Обучение и аттестация")) {
                                            Log.v(TAG, "authorize | success | authorized");
                                            responseHandler.onProgress(STATE_AUTHORIZED);
                                            responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                        } else {
                                            Log.v(TAG, "authorize | success | FAILED_AUTH_TRY_AGAIN");
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "authorize | failure"  + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), isInterrupted(throwable) ? FAILED_INTERRUPTED : FAILED_AUTH_TRY_AGAIN);
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                }
            }
        });
    }
    public static void get(final Context context, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, DEFAULT_RE_AUTH);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final ResponseHandler responseHandler) {
        get(context, protocol, url, query, responseHandler, DEFAULT_RE_AUTH);
    }
    public static void get(final Context context, final String url, final Map<String, String> query, final ResponseHandler responseHandler, final boolean reAuth) {
        get(context, DEFAULT_PROTOCOL, url, query, responseHandler, reAuth);
    }
    public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final ResponseHandler responseHandler, final boolean reAuth) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "get | url=" + url);
                if (Static.isOnline(context)) {
                    if (reAuth && checkJsessionId(context)) {
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
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | success | statusCode=" + code);
                                    try {
                                        if (code >= 500 && code < 600) {
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                            return;
                                        }
                                        if (response == null) throw new NullPointerException("data cannot be null");
                                        if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                                            Log.v(TAG, "get | url=" + url + " | success | auth required");
                                            if (reAuth) {
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
                                        Static.error(e);
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "get | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), isInterrupted(throwable) ? FAILED_INTERRUPTED : FAILED_TRY_AGAIN);
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                } else {
                    Log.v(TAG, "get | offline");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                }
            }
        });
    }
    public static void post(final Context context, final String url, final Map<String, String> params, final ResponseHandler responseHandler) {
        post(context, DEFAULT_PROTOCOL, url, params, responseHandler);
    }
    public static void post(final Context context, final Protocol protocol, final String url, final Map<String, String> params, final ResponseHandler responseHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "post | url=" + url);
                if (Static.isOnline(context)) {
                    if (checkJsessionId(context)) {
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
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "post | url=" + url + " | success | statusCode=" + code);
                                    try {
                                        if (code >= 500 && code < 600) {
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_SERVER_ERROR);
                                            return;
                                        }
                                        if (response == null) throw new NullPointerException("data cannot be null");
                                        if (response.contains("Закончился интервал неактивности") || response.contains("Доступ запрещен")) {
                                            Log.v(TAG, "p | success | auth required");
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
                                            responseHandler.onSuccess(code, new Headers(headers), response);
                                        }
                                    } catch (Exception e) {
                                        Static.error(e);
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                                    }
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    Log.v(TAG, "post | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), isInterrupted(throwable) ? FAILED_INTERRUPTED : FAILED_TRY_AGAIN);
                                }
                            });
                        }
                        @Override
                        public void onNewRequest(Request request) {
                            responseHandler.onNewRequest(request);
                        }
                    });
                } else {
                    Log.v(TAG, "p | offline");
                    responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                }
            }
        });
    }

    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getProtocol(protocol) + BASE_URL + "/" + relativeUrl;
    }
}
