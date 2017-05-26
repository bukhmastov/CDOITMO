package com.bukhmastov.cdoitmo.network.interfaces;

import android.graphics.drawable.Drawable;

import com.loopj.android.http.RequestHandle;

public interface DeIfmoDrawableClientResponseHandler {
    void onSuccess(int statusCode, Drawable drawable);
    void onProgress(int state);
    void onFailure(int state);
    void onNewHandle(RequestHandle requestHandle);
}
