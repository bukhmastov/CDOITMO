package com.bukhmastov.cdoitmo.network.handlers.joiner;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;

public class ResponseHandlerJoiner implements ResponseHandler {

    private final ResponseHandler handler;

    public ResponseHandlerJoiner(ResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onSuccess(int code, Client.Headers headers, String response) {
        handler.onSuccess(code, headers, response);
    }

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
