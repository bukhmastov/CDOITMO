package com.bukhmastov.cdoitmo.network.provider;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.provider.impl.NetworkUserAgentProviderImpl;

public interface NetworkUserAgentProvider {

    // future: replace with DI factory
    NetworkUserAgentProvider instance = new NetworkUserAgentProviderImpl();
    static NetworkUserAgentProvider instance() {
        return instance;
    }

    String get(Context context);
}
