package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface ResponseHandler {
    void onSuccess(final int statusCode, final Client.Headers headers, final String response);
    void onFailure(final int statusCode, final Client.Headers headers, final int state);
    void onProgress(final int state);
    void onNewRequest(final Client.Request request);
}
