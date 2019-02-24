package com.bukhmastov.cdoitmo.network.handlers.joiner;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;

public abstract class RestStringResponseHandlerJoiner implements ResponseHandler {

    private final RestResponseHandler<?> handler;

    public RestStringResponseHandlerJoiner(RestResponseHandler<?> handler) {
        this.handler = handler;
    }

    @Override
    public abstract void onSuccess(int code, Client.Headers headers, String response) throws Exception;

    @Override
    public void onFailure(int code, Client.Headers headers, int state) {
        handler.onFailure(code, headers, state);
    }

    @Override
    public void onProgress(int state) {
        handler.onProgress(state);
    }

    @Override
    public void onNewRequest(Client.Request request) {
        handler.onNewRequest(request);
    }
}
