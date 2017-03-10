package com.bukhmastov.cdoitmo.network.interfaces;

import com.loopj.android.http.RequestHandle;

import cz.msebera.android.httpclient.Header;

public interface Room101ClientResponseHandler {
    void onSuccess(int statusCode, String response);
    void onProgress(int state);
    void onFailure(int state, int statusCode, Header[] headers);
    void onNewHandle(RequestHandle requestHandle);
}