package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.HashMap;
import java.util.Map;

public abstract class Isu extends Client {

    public static final int STATE_CHECKING = 10;
    public static final int STATE_AUTHORIZATION = 11;
    public static final int STATE_AUTHORIZED = 12;
    public static final int FAILED_AUTH_TRY_AGAIN = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;

    // TODO add isu api keys, when isu will be ready
    private static final String API_KEY = "<api_key>";
    private static final String CLIENT_ID = "<client_id>";
    private static final String CLIENT_SECRET = "<client_secret>";
    protected static String getApiKey() {
        return API_KEY;
    }
    protected static String getClientId() {
        return CLIENT_ID;
    }
    protected static String getClientSecret() {
        return CLIENT_SECRET;
    }

    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _g(url, getHeaders(context), query, rawHandler);
                } catch (Throwable throwable) {
                    rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
                }
            }
        });
    }
    protected static void p(final Context context, final String url, final Map<String, String> query, final Map<String, String> params, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _p(url, getHeaders(context), query, params, rawHandler);
                } catch (Throwable throwable) {
                    rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
                }
            }
        });
    }
    protected static void gJson(final Context context, final String url, final Map<String, String> query, final RawJsonHandler rawJsonHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _gJson(url, getHeaders(context), query, rawJsonHandler);
                } catch (Throwable throwable) {
                    rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
                }
            }
        });
    }
    private static okhttp3.Headers getHeaders(final Context context) throws Throwable {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Static.getUserAgent(context));
        return okhttp3.Headers.of(headers);
    }
}
