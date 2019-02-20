package com.bukhmastov.cdoitmo.network.handlers.joiner;

import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public class RestResponseHandlerJoiner implements RestResponseHandler {

    private final RestResponseHandler handler;

    public RestResponseHandlerJoiner(RestResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr) {
        handler.onSuccess(code, headers, obj, arr);
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
