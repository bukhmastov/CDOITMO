package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.provider.StorageProvider;

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

    @Provides
    @Singleton
    public StorageProvider provideStorageProvider() {
        return new StorageProvider();
    }
}
