package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.network.DeIfmoClient;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.network.Room101Client;
import com.bukhmastov.cdoitmo.network.impl.DeIfmoClientImpl;
import com.bukhmastov.cdoitmo.network.impl.DeIfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.impl.IfmoClientImpl;
import com.bukhmastov.cdoitmo.network.impl.IfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.impl.IsuRestClientImpl;
import com.bukhmastov.cdoitmo.network.impl.Room101ClientImpl;
import com.bukhmastov.cdoitmo.network.provider.NetworkClientProvider;
import com.bukhmastov.cdoitmo.network.provider.NetworkUserAgentProvider;
import com.bukhmastov.cdoitmo.network.provider.impl.NetworkClientProviderImpl;
import com.bukhmastov.cdoitmo.network.provider.impl.NetworkUserAgentProviderImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    public DeIfmoClient provideDeIfmoClient() {
        return new DeIfmoClientImpl();
    }

    @Provides
    @Singleton
    public DeIfmoRestClient provideDeIfmoRestClient() {
        return new DeIfmoRestClientImpl();
    }

    @Provides
    @Singleton
    public IfmoClient provideIfmoClient() {
        return new IfmoClientImpl();
    }

    @Provides
    @Singleton
    public IfmoRestClient provideIfmoRestClient() {
        return new IfmoRestClientImpl();
    }

    @Provides
    @Singleton
    public IsuRestClient provideIsuRestClient() {
        return new IsuRestClientImpl();
    }

    @Provides
    @Singleton
    public Room101Client provideRoom101Client() {
        return new Room101ClientImpl();
    }

    @Provides
    @Singleton
    public NetworkClientProvider provideNetworkClientProvider() {
        return new NetworkClientProviderImpl();
    }

    @Provides
    @Singleton
    public NetworkUserAgentProvider provideNetworkUserAgentProvider() {
        return new NetworkUserAgentProviderImpl();
    }
}
