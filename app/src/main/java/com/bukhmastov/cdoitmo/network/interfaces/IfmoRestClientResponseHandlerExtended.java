package com.bukhmastov.cdoitmo.network.interfaces;

public interface IfmoRestClientResponseHandlerExtended extends IfmoRestClientResponseHandler {
    void onFailure(int statusCode, int state);
}
