package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidgetStorage;
import com.bukhmastov.cdoitmo.widget.impl.ScheduleLessonsWidgetStorageImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class WidgetModule {

    @Provides
    @Singleton
    public ScheduleLessonsWidgetStorage provideScheduleLessonsWidgetStorage() {
        return new ScheduleLessonsWidgetStorageImpl();
    }
}
