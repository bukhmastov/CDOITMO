package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.impl.EventBusImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class EventBusModule {

    @Provides
    @Singleton
    public EventBus provideBus() {
        return new EventBusImpl();
    }
}
