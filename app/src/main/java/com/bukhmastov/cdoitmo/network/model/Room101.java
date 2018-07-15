package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.RawHandler;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Map;

public abstract class Room101 extends DeIfmo {

    public static final int FAILED_AUTH = 10;
    public static final int FAILED_EXPECTED_REDIRECTION = 11;

    @Override
    protected void g(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> query, @NonNull final RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            try {
                _g(url, getHeaders(context), query, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        thread.run(thread.BACKGROUND, () -> {
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

    @Override
    protected void p(@NonNull final Context context, @NonNull final String url, @Nullable final Map<String, String> params, @NonNull final RawHandler rawHandler) {
        thread.run(thread.BACKGROUND, () -> {
            try {
                _p(url, getHeaders(context), null, params, new RawHandler() {
                    @Override
                    public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                        thread.run(thread.BACKGROUND, () -> {
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
