package com.bukhmastov.cdoitmo.network.interfaces;

import com.loopj.android.http.RequestHandle;

public interface DeIfmoClientResponseHandler {
    void onSuccess(int statusCode, String response);
    void onProgress(int state);
    void onFailure(int statusCode, int state);
    void onNewHandle(RequestHandle requestHandle);
}