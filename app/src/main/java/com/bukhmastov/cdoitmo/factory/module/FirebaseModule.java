package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseAnalyticsProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseConfigProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseCrashlyticsProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebasePerformanceProviderImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class FirebaseModule {

    @Provides
    @Singleton
    public FirebaseAnalyticsProvider provideFirebaseAnalyticsProvider() {
        return new FirebaseAnalyticsProviderImpl();
    }

    @Provides
    @Singleton
    public FirebaseConfigProvider provideFirebaseConfigProvider() {
        return new FirebaseConfigProviderImpl();
    }

    @Provides
    @Singleton
    public FirebaseCrashlyticsProvider provideFirebaseCrashlyticsProvider() {
        return new FirebaseCrashlyticsProviderImpl();
    }

    @Provides
    @Singleton
    public FirebasePerformanceProvider provideFirebasePerformanceProvider() {
        return new FirebasePerformanceProviderImpl();
    }
}
