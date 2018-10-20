package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public interface RestResponseHandler {
    void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject obj, final JSONArray arr);
    void onFailure(final int statusCode, final Client.Headers headers, final int state);
    void onProgress(final int state);
    void onNewRequest(final Client.Request request);
}
