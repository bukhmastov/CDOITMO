package com.bukhmastov.cdoitmo.network.provider;

import com.bukhmastov.cdoitmo.network.provider.impl.NetworkClientProviderImpl;

import okhttp3.OkHttpClient;

public interface NetworkClientProvider {

    // future: replace with DI factory
    NetworkClientProvider instance = new NetworkClientProviderImpl();
    static NetworkClientProvider instance() {
        return instance;
    }

    OkHttpClient get();
}
