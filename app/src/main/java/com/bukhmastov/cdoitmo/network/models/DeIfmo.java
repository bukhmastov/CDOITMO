package com.bukhmastov.cdoitmo.network.models;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.HashMap;
import java.util.Map;

public abstract class DeIfmo extends Client {

    private static final long jsessionid_ts_limit = 1200000L; // 20min // 20 * 60 * 1000

    public static final int STATE_CHECKING = 10;
    public static final int STATE_AUTHORIZATION = 11;
    public static final int STATE_AUTHORIZED = 12;
    public static final int FAILED_AUTH_TRY_AGAIN = 10;
    public static final int FAILED_AUTH_CREDENTIALS_REQUIRED = 11;
    public static final int FAILED_AUTH_CREDENTIALS_FAILED = 12;

    protected static boolean checkJsessionId(final Context context) {
        return Long.parseLong(Storage.file.perm.get(context, "user#jsessionid_ts", "0")) + jsessionid_ts_limit < System.currentTimeMillis();
    }
    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    _g(url, getHeaders(context), query, rawHandler);
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
                    _p(url, getHeaders(context), params, rawHandler);
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
                    _gJson(url, getHeaders(context), query, rawJsonHandler);
                } catch (Throwable throwable) {
                    rawJsonHandler.onError(throwable);
                }
            }
        });
    }

    private static okhttp3.Headers getHeaders(final Context context) throws Throwable {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Static.getUserAgent(context));
        headers.put("Cookie", "JSESSIONID=" + Storage.file.perm.get(context, "user#jsessionid") + "; Path=/;");
        return okhttp3.Headers.of(headers);
    }
}
