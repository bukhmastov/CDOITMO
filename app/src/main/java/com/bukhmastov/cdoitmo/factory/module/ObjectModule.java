package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.object.DaysRemainingWidget;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.object.ProtocolTrackerService;
import com.bukhmastov.cdoitmo.object.Room101AddRequest;
import com.bukhmastov.cdoitmo.object.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.object.SettingsScheduleExams;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.object.TimeRemainingWidget;
import com.bukhmastov.cdoitmo.object.impl.DaysRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerImpl;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerServiceImpl;
import com.bukhmastov.cdoitmo.object.impl.Room101AddRequestImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.object.impl.TimeRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleAttestations;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleExams;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsHelperImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ObjectModule {

    @Provides
    @Singleton
    public DaysRemainingWidget provideDaysRemainingWidget() {
        return new DaysRemainingWidgetImpl();
    }

    @Provides
    @Singleton
    public ProtocolTracker provideProtocolTracker() {
        return new ProtocolTrackerImpl();
    }

    @Provides
    @Singleton
    public ProtocolTrackerService provideProtocolTrackerService() {
        return new ProtocolTrackerServiceImpl();
    }

    @Provides
    @Singleton
    public Room101AddRequest provideRoom101AddRequest() {
        return new Room101AddRequestImpl();
    }

    @Provides
    @Singleton
    public SettingsScheduleAttestations provideSettingsScheduleAttestations() {
        return new SettingsScheduleAttestationsImpl();
    }

    @Provides
    @Singleton
    public SettingsScheduleExams provideSettingsScheduleExams() {
        return new SettingsScheduleExamsImpl();
    }

    @Provides
    @Singleton
    public SettingsScheduleLessons provideSettingsScheduleLessons() {
        return new SettingsScheduleLessonsImpl();
    }

    @Provides
    @Singleton
    public TimeRemainingWidget provideTimeRemainingWidget() {
        return new TimeRemainingWidgetImpl();
    }

    @Provides
    public ScheduleLessons provideScheduleLessons() {
        return new ScheduleLessonsImpl();
    }

    @Provides
    public ScheduleExams provideScheduleExams() {
        return new ScheduleExamsImpl();
    }

    @Provides
    public ScheduleAttestations provideScheduleAttestations() {
        return new ScheduleAttestationsImpl();
    }

    @Provides
    @Singleton
    public ScheduleLessonsHelper provideScheduleLessonsHelper() {
        return new ScheduleLessonsHelperImpl();
    }
}
