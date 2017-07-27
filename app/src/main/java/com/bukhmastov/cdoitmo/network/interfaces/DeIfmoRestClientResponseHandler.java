package com.bukhmastov.cdoitmo.network.interfaces;

import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

public interface DeIfmoRestClientResponseHandler {
    void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr);
    void onProgress(int state);
    void onFailure(int statusCode, int state);
    void onNewHandle(RequestHandle requestHandle);
}