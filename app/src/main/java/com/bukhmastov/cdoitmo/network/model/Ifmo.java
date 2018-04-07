package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.network.interfaces.RawJsonHandler;
import com.bukhmastov.cdoitmo.util.Static;

import java.util.HashMap;
import java.util.Map;

public abstract class Ifmo extends Client {

    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            try {
                _g(url, getHeaders(context), query, rawHandler);
            } catch (Throwable throwable) {
                rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }
    protected static void p(final Context context, final String url, final Map<String, String> params, final RawHandler rawHandler) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            try {
                _p(url, getHeaders(context), null, params, rawHandler);
            } catch (Throwable throwable) {
                rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }
    protected static void gJson(final Context context, final String url, final Map<String, String> query, final RawJsonHandler rawJsonHandler) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            try {
                _gJson(url, getHeaders(context), query, rawJsonHandler);
            } catch (Throwable throwable) {
                rawJsonHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }

    private static okhttp3.Headers getHeaders(final Context context) throws Throwable {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", Static.getUserAgent(context));
        return okhttp3.Headers.of(headers);
    }
}
