package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.provider.InjectProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ProviderModule {

    @Provides
    @Singleton
    public InjectProvider provideInjectProvider() {
        return new InjectProvider();
    }
}
