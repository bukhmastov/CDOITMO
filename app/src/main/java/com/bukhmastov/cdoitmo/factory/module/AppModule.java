package com.bukhmastov.cdoitmo.factory.module;

import android.content.Context;
import androidx.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private Context appContext;

    public AppModule(@NonNull Context appContext) {
        this.appContext = appContext;
    }

    @Provides
    @Singleton
    public Context provideAppContext() {
        return appContext;
    }
}
