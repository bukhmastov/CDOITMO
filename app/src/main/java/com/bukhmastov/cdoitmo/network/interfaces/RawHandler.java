package com.bukhmastov.cdoitmo.network.interfaces;

import com.bukhmastov.cdoitmo.network.models.Client;

public interface RawHandler {
    void onDone(final int code, final okhttp3.Headers headers, final String response);
    void onError(final Throwable throwable);
    void onNewRequest(final Client.Request request);
}
