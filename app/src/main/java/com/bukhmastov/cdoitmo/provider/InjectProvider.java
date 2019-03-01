package com.bukhmastov.cdoitmo.provider;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.object.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.object.SettingsScheduleExams;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

import dagger.Lazy;

/**
 * Not recommended to use. Some day it will be gone, but who knows when...
 */
public class InjectProvider {

    @Inject
    Lazy<Log> log;
    @Inject
    Lazy<Thread> thread;
    @Inject
    Lazy<EventBus> eventBus;
    @Inject
    Lazy<Storage> storage;
    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    Lazy<ProtocolTracker> protocolTracker;
    @Inject
    Lazy<SettingsScheduleLessons> settingsScheduleLessons;
    @Inject
    Lazy<SettingsScheduleExams> settingsScheduleExams;
    @Inject
    Lazy<SettingsScheduleAttestations> settingsScheduleAttestations;
    @Inject
    Lazy<Account> account;
    @Inject
    Lazy< NotificationMessage> notificationMessage;
    @Inject
    Lazy<Static> staticUtil;
    @Inject
    Lazy<Theme> theme;
    @Inject
    Lazy<Time> time;

    public InjectProvider() {
        AppComponentProvider.getComponent().inject(this);
    }

    public Log getLog() {
        return log.get();
    }

    public Thread getThread() {
        return thread.get();
    }

    public EventBus getEventBus() {
        return eventBus.get();
    }

    public Storage getStorage() {
        return storage.get();
    }

    public StoragePref getStoragePref() {
        return storagePref.get();
    }

    public ProtocolTracker getProtocolTracker() {
        return protocolTracker.get();
    }

    public SettingsScheduleLessons getSettingsScheduleLessons() {
        return settingsScheduleLessons.get();
    }

    public SettingsScheduleExams getSettingsScheduleExams() {
        return settingsScheduleExams.get();
    }

    public SettingsScheduleAttestations getSettingsScheduleAttestations() {
        return settingsScheduleAttestations.get();
    }

    public Account getAccount() {
        return account.get();
    }

    public NotificationMessage getNotificationMessage() {
        return notificationMessage.get();
    }

    public Static getStaticUtil() {
        return staticUtil.get();
    }

    public Theme getTheme() {
        return theme.get();
    }

    public Time getTime() {
        return time.get();
    }
}
