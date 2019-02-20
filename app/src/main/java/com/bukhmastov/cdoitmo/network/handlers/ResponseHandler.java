package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface ResponseHandler extends ResponseHasFailed {
    void onSuccess(int code, Client.Headers headers, String response);
    void onFailure(int code, Client.Headers headers, int state);
    void onProgress(int state);
    void onNewRequest(Client.Request request);
}
