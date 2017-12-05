package com.bukhmastov.cdoitmo.network.interfaces;

import com.bukhmastov.cdoitmo.network.models.Client;

import org.json.JSONArray;
import org.json.JSONObject;

public interface RawJsonHandler {
    void onDone(final int code, final okhttp3.Headers headers, final String response, final JSONObject responseObj, final JSONArray responseArr);
    void onError(final int code, final okhttp3.Headers headers, final Throwable throwable);
    void onNewRequest(final Client.Request request);
}
