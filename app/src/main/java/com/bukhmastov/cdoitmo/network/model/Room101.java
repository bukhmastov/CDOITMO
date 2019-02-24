package com.bukhmastov.cdoitmo.network.model;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.joiner.ResponseHandlerJoiner;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Room101 extends DeIfmo {

    public Room101() {
        super();
    }

    @Override
    protected void doGet(@NonNull Context context, @NonNull String url,
                         @Nullable Map<String, String> query, @NonNull ResponseHandler handler) {
        try {
            doGet(url, getHeaders(context), query, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    storeCookies(context, headers.get(), false);
                    super.onSuccess(code, headers, response);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    storeCookies(context, headers.get(), false);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }

    @Override
    protected void doPost(@NonNull Context context, @NonNull String url,
                          @Nullable Map<String, String> params, @NonNull ResponseHandler handler) {
        try {
            doPost(url, getHeaders(context), null, params, new ResponseHandlerJoiner(handler) {
                @Override
                public void onSuccess(int code, Headers headers, String response) throws Exception {
                    storeCookies(context, headers.get(), false);
                    super.onSuccess(code, headers, response);
                }
                @Override
                public void onFailure(int code, Headers headers, int state) {
                    storeCookies(context, headers.get(), false);
                    super.onFailure(code, headers, state);
                }
            });
        } catch (Exception exception) {
            handler.onFailure(STATUS_CODE_EMPTY, null, getFailedStatus(exception));
        }
    }
}
