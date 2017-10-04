package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Room101 extends Client {

    public static final int FAILED_AUTH = 10;
    public static final int FAILED_EXPECTED_REDIRECTION = 11;

    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _g(url, getHeaders(context), query, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    analyseCookies(context, headers);
                                    rawHandler.onDone(code, headers, response);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            rawHandler.onError(throwable);
                        }
                        @Override
                        public void onNewRequest(final Request request) {
                            rawHandler.onNewRequest(request);
                        }
                    });
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void p(final Context context, final String url, final Map<String, String> params, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _p(url, getHeaders(context), null, params, new RawHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    analyseCookies(context, headers);
                                    rawHandler.onDone(code, headers, response);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            rawHandler.onError(throwable);
                        }
                        @Override
                        public void onNewRequest(final Request request) {
                            rawHandler.onNewRequest(request);
                        }
                    });
                } catch (Throwable throwable) {
                    rawHandler.onError(throwable);
                }
            }
        });
    }
    protected static void gJson(final Context context, final String url, final Map<String, String> query, final RawJsonHandler rawJsonHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _gJson(url, getHeaders(context), query, new RawJsonHandler() {
                        @Override
                        public void onDone(final int code, final okhttp3.Headers headers, final String response, final JSONObject responseObj, final JSONArray responseArr) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    analyseCookies(context, headers);
                                    rawJsonHandler.onDone(code, headers, response, responseObj, responseArr);
                                }
                            });
                        }
                        @Override
                        public void onError(final Throwable throwable) {
                            rawJsonHandler.onError(throwable);
                        }
                        @Override
                        public void onNewRequest(final Request request) {
                            rawJsonHandler.onNewRequest(request);
                        }
                    });
                } catch (Throwable throwable) {
                    rawJsonHandler.onError(throwable);
                }
            }
        });
    }

    private static okhttp3.Headers getHeaders(final Context context) throws Throwable {
        HashMap<String, String> headers = new HashMap<>();
        String phpsessid = Storage.file.perm.get(context, "user#phpsessid", "");
        headers.put("User-Agent", Static.getUserAgent(context));
        if (phpsessid != null && !phpsessid.isEmpty()) {
            headers.put("Cookie", "PHPSESSID=" + Storage.file.perm.get(context, "user#phpsessid") + "; autoexit=true;");
        }
        return okhttp3.Headers.of(headers);
    }
    private static void analyseCookies(Context context, okhttp3.Headers headers) {
        if (headers == null) {
            return;
        }
        String value = headers.get("cookie");
        if (value == null) {
            value = headers.get("set-cookie");
        }
        if (value != null) {
            String[] entities = value.split(";");
            for (String entity : entities) {
                String[] cookie = entity.split("=");
                if (Objects.equals(cookie[0], "PHPSESSID") && !Objects.equals(cookie[1], "") && cookie[1] != null) {
                    Storage.file.perm.put(context, "user#phpsessid", cookie[1]);
                }
            }
        }
    }
}
