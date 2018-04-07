package com.bukhmastov.cdoitmo.network.interfaces;

import com.bukhmastov.cdoitmo.network.model.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public interface RestResponseHandler {
    void onSuccess(final int statusCode, final Client.Headers headers, final JSONObject responseObj, final JSONArray responseArr);
    void onFailure(final int statusCode, final Client.Headers headers, final int state);
    void onProgress(final int state);
    void onNewRequest(final Client.Request request);
}
