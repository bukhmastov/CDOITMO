package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface RawHandler {
    void onDone(int code, okhttp3.Headers headers, String response) throws Exception;
    void onError(int code, okhttp3.Headers headers, Throwable throwable);
    void onNewRequest(Client.Request request);
}
