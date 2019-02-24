package com.bukhmastov.cdoitmo.network.handlers.joiner;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class RestRestResponseHandlerJoiner<J extends JsonEntity, T extends JsonEntity> implements RestResponseHandler<T> {

    private final RestResponseHandler<J> handler;

    public RestRestResponseHandlerJoiner(RestResponseHandler<J> handler) {
        this.handler = handler;
    }

    @Override
    public abstract void onSuccess(int code, Client.Headers headers, T response) throws Exception;

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

    @Override
    public abstract T newInstance();

    @Override
    public JSONObject convertArray(JSONArray arr) throws JSONException {
        return handler.convertArray(arr);
    }
}
