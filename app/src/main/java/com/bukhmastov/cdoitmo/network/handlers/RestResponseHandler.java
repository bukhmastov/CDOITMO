package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public interface RestResponseHandler extends ResponseHasFailed {
    void onSuccess(int code, Client.Headers headers, JSONObject obj, JSONArray arr);
    void onFailure(int code, Client.Headers headers, int state);
    void onProgress(int state);
    void onNewRequest(Client.Request request);
}
