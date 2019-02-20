package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.network.handlers.RawHandler;

import java.util.Map;

public abstract class Room101 extends DeIfmo {

    public static final int FAILED_AUTH = 10;
    public static final int FAILED_EXPECTED_REDIRECTION = 11;

    public Room101() {
        super();
    }

    @Override
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull RawHandler rawHandler) {
        try {
            doGet(url, getHeaders(context), query, new RawHandler() {
                @Override
                public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                    try {
                        storeCookies(context, headers, false);
                        rawHandler.onDone(code, headers, response);
                    } catch (Throwable throwable) {
                        rawHandler.onError(code, headers, throwable);
                    }
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
    }

    @Override
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull RawHandler rawHandler) {
        try {
            doPost(url, getHeaders(context), null, params, new RawHandler() {
                @Override
                public void onDone(final int code, final okhttp3.Headers headers, final String response) {
                    try {
                        storeCookies(context, headers, false);
                        rawHandler.onDone(code, headers, response);
                    } catch (Throwable throwable) {
                        rawHandler.onError(code, headers, throwable);
                    }
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
    }
}
