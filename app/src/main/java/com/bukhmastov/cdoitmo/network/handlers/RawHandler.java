package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface RawHandler {
    void onDone(final int code, final okhttp3.Headers headers, final String response);
    void onError(final int code, final okhttp3.Headers headers, final Throwable throwable);
    void onNewRequest(final Client.Request request);
}
