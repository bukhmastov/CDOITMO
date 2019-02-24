package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface ResponseHandler {
    void onSuccess(int code, Client.Headers headers, String response) throws Exception;
    void onFailure(int code, Client.Headers headers, int state);
    void onProgress(int state);
    void onNewRequest(Client.Request request);
}
