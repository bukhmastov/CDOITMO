package com.bukhmastov.cdoitmo.network.provider;

import okhttp3.OkHttpClient;

public interface NetworkClientProvider {

    OkHttpClient get();
}
