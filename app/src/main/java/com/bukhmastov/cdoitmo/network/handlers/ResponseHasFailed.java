package com.bukhmastov.cdoitmo.network.handlers;

import com.bukhmastov.cdoitmo.network.model.Client;

public interface ResponseHasFailed {
    void onFailure(int code, Client.Headers headers, int state);
}
