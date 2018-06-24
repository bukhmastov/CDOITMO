package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.interfaces.RawHandler;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

//TODO interface - impl
public abstract class Room101 extends DeIfmo {

    public static final int FAILED_AUTH = 10;
    public static final int FAILED_EXPECTED_REDIRECTION = 11;

    protected static void g(final Context context, final String url, final Map<String, String> query, final RawHandler rawHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            try {
                _g(url, getHeaders(context), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            storeCookies(context, headers, false);
                            rawHandler.onDone(code, headers, response);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        rawHandler.onError(code, headers, throwable);
                    }
                    @Override
                    public void onNewRequest(final Request request) {
                        rawHandler.onNewRequest(request);
                    }
                });
            } catch (Throwable throwable) {
                rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }
    protected static void p(final Context context, final String url, final Map<String, String> params, final RawHandler rawHandler) {
        Thread.run(Thread.BACKGROUND, () -> {
            try {
                _p(url, getHeaders(context), null, params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        Thread.run(Thread.BACKGROUND, () -> {
                            storeCookies(context, headers, false);
                            rawHandler.onDone(code, headers, response);
                        });
                    }
                    @Override
                    public void onError(final int code, final okhttp3.Headers headers, final Throwable throwable) {
                        rawHandler.onError(code, headers, throwable);
                    }
                    @Override
                    public void onNewRequest(final Request request) {
                        rawHandler.onNewRequest(request);
                    }
                });
            } catch (Throwable throwable) {
                rawHandler.onError(STATUS_CODE_EMPTY, null, throwable);
            }
        });
    }
}
