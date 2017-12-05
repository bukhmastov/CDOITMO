package com.bukhmastov.cdoitmo.network;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.network.interfaces.ResponseHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.network.models.Isu;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class IsuRestClient extends Isu {

    private static final String TAG = "IsuRestClient";
    private static final String BASE_URL = "isu.ifmo.ru/ords/isurest/v1/api/core";
    private static final String BASE_URL_AUTH = "services.ifmo.ru:8444/cas/oauth2.0";
    private static final Client.Protocol DEFAULT_PROTOCOL = Client.Protocol.HTTPS;
    private static final Client.Protocol DEFAULT_PROTOCOL_AUTH = Client.Protocol.HTTPS;

    public static class Private {
        public static void check(final Context context, final ResponseHandler responseHandler) {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "check");
                    if (Static.isOnline(context)) {
                        responseHandler.onProgress(STATE_CHECKING);
                        final String access_token = Storage.file.perm.get(context, "user#isu#access_token", "").trim();
                        final String refresh_token = Storage.file.perm.get(context, "user#isu#refresh_token", "").trim();
                        final long expires_at = Long.parseLong(Storage.file.perm.get(context, "user#isu#expires_at", "0").trim());
                        if (expires_at < System.currentTimeMillis()) {
                            if (!refresh_token.isEmpty()) {
                                Log.v(TAG, "check | refresh token expired, going to retrieve new access token");
                                authorize(context, new ResponseHandler() {
                                    @Override
                                    public void onSuccess(final int statusCode, final Headers headers, final String response) {
                                        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                            @Override
                                            public void run() {
                                                if ("authorized".equals(response)) {
                                                    Log.v(TAG, "check | authorize | all systems operational");
                                                    responseHandler.onProgress(STATE_AUTHORIZED);
                                                    responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                                                } else {
                                                    Log.v(TAG, "check | authorize | failed | statusCode=" + statusCode + " | response=" + response);
                                                    responseHandler.onFailure(statusCode, headers, FAILED_AUTH_TRY_AGAIN);
                                                }
                                            }
                                        });
                                    }
                                    @Override
                                    public void onFailure(final int statusCode, final Headers headers, final int state) {
                                        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.v(TAG, "check | authorize | failed | statusCode=" + statusCode + " | state=" + state);
                                                responseHandler.onFailure(statusCode, headers, state);
                                            }
                                        });
                                    }
                                    @Override
                                    public void onProgress(int state) {}
                                    @Override
                                    public void onNewRequest(Request request) {
                                        responseHandler.onNewRequest(request);
                                    }
                                });
                            } else {
                                Log.v(TAG, "check | refresh token is empty");
                                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                            }
                        } else {
                            if (!access_token.isEmpty()) {
                                Log.v(TAG, "check | all systems operational");
                                responseHandler.onProgress(STATE_AUTHORIZED);
                                responseHandler.onSuccess(STATUS_CODE_EMPTY, new Headers(null), "authorized");
                            } else {
                                Log.v(TAG, "check | access token is empty");
                                responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                            }
                        }
                    } else {
                        responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                    }
                }
            });
        }
        public static void authorize(final Context context, final String username, final String password, final ResponseHandler responseHandler) {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "authorize by password");
                    responseHandler.onProgress(STATE_AUTHORIZATION);
                    if (username.isEmpty() || password.isEmpty()) {
                        Log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                        responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                    } else {
                        HashMap<String, String> query = new HashMap<>();
                        query.put("grant_type", "password");
                        query.put("client_id", getClientId());
                        query.put("username", username);
                        query.put("password", password);
                        p(context, getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken"), query, null, new RawHandler() {
                            @Override
                            public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.v(TAG, "authorize by password | success | code=" + code);
                                            if (code == 200) {
                                                final String access_token = headers.get("access_token");
                                                final String expires_in = headers.get("expires_in");
                                                final String refresh_token = headers.get("refresh_token");
                                                if (access_token == null || refresh_token == null) {
                                                    responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                                    return;
                                                }
                                                Storage.file.perm.put(context, "user#isu#access_token", access_token);
                                                Storage.file.perm.put(context, "user#isu#refresh_token", refresh_token);
                                                try {
                                                    Storage.file.perm.put(context, "user#isu#expires_at", String.valueOf((Long.parseLong(expires_in)) * 1000L + System.currentTimeMillis() - 60000L));
                                                } catch (Exception ignore) {
                                                    Storage.file.perm.put(context, "user#isu#expires_at", String.valueOf(System.currentTimeMillis() + 1800000L)); // 30min
                                                }
                                                responseHandler.onProgress(STATE_AUTHORIZED);
                                                responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                            } else {
                                                switch (code) {
                                                    case 401: {
                                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                                                        break;
                                                    }
                                                    default: {
                                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.v(TAG, "authorize by password | success | exception | e=" + e.getMessage());
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "authorize by password | failure"  + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
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
        public static void authorize(final Context context, final ResponseHandler responseHandler) {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "authorize by refresh token");
                    responseHandler.onProgress(STATE_AUTHORIZATION);
                    final String refresh_token = Storage.file.perm.get(context, "user#isu#refresh_token", "").trim();
                    if (refresh_token.isEmpty()) {
                        Log.v(TAG, "authorize | FAILED_AUTH_CREDENTIALS_REQUIRED");
                        responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_AUTH_CREDENTIALS_REQUIRED);
                    } else {
                        HashMap<String, String> query = new HashMap<>();
                        query.put("grant_type", "refresh_token");
                        query.put("client_id", getClientId());
                        query.put("client_secret", getClientSecret());
                        query.put("refresh_token", refresh_token);
                        p(context, getAbsoluteUrl(DEFAULT_PROTOCOL_AUTH, BASE_URL_AUTH, "accessToken"), query, null, new RawHandler() {
                            @Override
                            public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Log.v(TAG, "authorize by refresh token | success | code=" + code);
                                            if (code == 200) {
                                                final String access_token = headers.get("access_token");
                                                final String expires_in = headers.get("expires_in");
                                                if (access_token == null) {
                                                    responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                                    return;
                                                }
                                                Storage.file.perm.put(context, "user#isu#access_token", access_token);
                                                try {
                                                    Storage.file.perm.put(context, "user#isu#expires_at", String.valueOf((Long.parseLong(expires_in)) * 1000L + System.currentTimeMillis() - 60000L));
                                                } catch (Exception ignore) {
                                                    Storage.file.perm.put(context, "user#isu#expires_at", String.valueOf(System.currentTimeMillis() + 1800000L)); // 30min
                                                }
                                                responseHandler.onProgress(STATE_AUTHORIZED);
                                                responseHandler.onSuccess(code, new Headers(headers), "authorized");
                                            } else {
                                                switch (code) {
                                                    case 401: {
                                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_CREDENTIALS_FAILED);
                                                        break;
                                                    }
                                                    default: {
                                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.v(TAG, "authorize by refresh token | success | exception | e=" + e.getMessage());
                                            responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "authorize by refresh token | failure"  + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_AUTH_TRY_AGAIN);
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
        public static void get(final Context context, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
            get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
        }
        public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "get | url=" + url);
                    if (Static.isOnline(context)) {
                        check(context, new ResponseHandler() {
                            @Override
                            public void onSuccess(final int statusCode, final Headers headers, final String response) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        if ("authorized".equals(response)) {
                                            responseHandler.onProgress(STATE_HANDLING);
                                            gJson(context, getAbsoluteUrl(protocol, getUrl(context, url)), query, new RawJsonHandler() {
                                                @Override
                                                public void onDone(final int code, final okhttp3.Headers headers, String response, final JSONObject responseObj, final JSONArray responseArr) {
                                                    Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Log.v(TAG, "get | url=" + url + " | success | statusCode=" + code);
                                                            responseHandler.onSuccess(code, new Headers(headers), responseObj, responseArr);
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                                                    Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Log.v(TAG, "get | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                                            responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
                                                        }
                                                    });
                                                }
                                                @Override
                                                public void onNewRequest(Request request) {
                                                    responseHandler.onNewRequest(request);
                                                }
                                            });
                                        } else {
                                            Log.v(TAG, "get | check failed | statusCode=" + statusCode + " | response=" + response);
                                            responseHandler.onFailure(statusCode, headers, FAILED_AUTH_CREDENTIALS_REQUIRED);
                                        }
                                    }
                                });
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
                    } else {
                        Log.v(TAG, "get | offline");
                        responseHandler.onFailure(STATUS_CODE_EMPTY, new Headers(null), FAILED_OFFLINE);
                    }
                }
            });
        }
    }
    public static class Public {
        public static void get(final Context context, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
            get(context, DEFAULT_PROTOCOL, url, query, responseHandler);
        }
        public static void get(final Context context, final Protocol protocol, final String url, final Map<String, String> query, final RestResponseHandler responseHandler) {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "get | url=" + url);
                    if (Static.isOnline(context)) {
                        responseHandler.onProgress(STATE_HANDLING);
                        gJson(context, getAbsoluteUrl(protocol, getUrl(context, url)), query, new RawJsonHandler() {
                            @Override
                            public void onDone(final int code, final okhttp3.Headers headers, String response, final JSONObject responseObj, final JSONArray responseArr) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "get | url=" + url + " | success | statusCode=" + code);
                                        responseHandler.onSuccess(code, new Headers(headers), responseObj, responseArr);
                                    }
                                });
                            }
                            @Override
                            public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                                Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.v(TAG, "get | url=" + url + " | failure" + (throwable != null ? " | throwable=" + throwable.getMessage() : ""));
                                        responseHandler.onFailure(code, new Headers(headers), FAILED_TRY_AGAIN);
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
    }

    public static boolean isAuthorized(final Context context) {
        final String access_token = Storage.file.perm.get(context, "user#isu#access_token", "").trim();
        final String refresh_token = Storage.file.perm.get(context, "user#isu#refresh_token", "").trim();
        return !access_token.isEmpty() && !refresh_token.isEmpty();
    }

    private static String getUrl(Context context, String url) {
        return url.replace("%key%", getApiKey()).replace("%token%", Storage.file.perm.get(context, "user#isu#access_token"));
    }
    private static String getAbsoluteUrl(Protocol protocol, String relativeUrl) {
        return getAbsoluteUrl(protocol, BASE_URL, relativeUrl);
    }
    private static String getAbsoluteUrl(Protocol protocol, String base, String relativeUrl) {
        return getProtocol(protocol) + base + "/" + relativeUrl;
    }
}
