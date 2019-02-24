package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface RestResponseHandler<T extends JsonEntity> {
    void onSuccess(int code, Client.Headers headers, T response) throws Exception;
    void onFailure(int code, Client.Headers headers, int state);
    void onProgress(int state);
    void onNewRequest(Client.Request request);
    T newInstance();
    default JSONObject convertArray(JSONArray arr) throws JSONException {
        return null;
    }
}
