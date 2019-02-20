package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public interface RawJsonHandler {
    void onDone(int code, okhttp3.Headers headers, String response, JSONObject obj, JSONArray arr);
    void onError(int code, okhttp3.Headers headers, Throwable throwable);
    void onNewRequest(Client.Request request);
}
