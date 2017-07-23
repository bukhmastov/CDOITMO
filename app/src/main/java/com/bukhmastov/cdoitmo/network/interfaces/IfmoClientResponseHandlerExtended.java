package com.bukhmastov.cdoitmo.network.interfaces;

public interface IfmoClientResponseHandlerExtended extends IfmoClientResponseHandler {
    void onFailure(int statusCode, int state);
}
