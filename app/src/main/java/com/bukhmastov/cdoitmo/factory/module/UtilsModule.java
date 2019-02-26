package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Accounts;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NavigationMenu;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StorageLocalCache;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.DateUtils;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.impl.AccountImpl;
import com.bukhmastov.cdoitmo.util.impl.AccountsImpl;
import com.bukhmastov.cdoitmo.util.impl.DateUtilsImpl;
import com.bukhmastov.cdoitmo.util.impl.LogImpl;
import com.bukhmastov.cdoitmo.util.impl.NavigationMenuImpl;
import com.bukhmastov.cdoitmo.util.impl.NotificationMessageImpl;
import com.bukhmastov.cdoitmo.util.impl.NotificationsImpl;
import com.bukhmastov.cdoitmo.util.impl.StaticImpl;
import com.bukhmastov.cdoitmo.util.impl.StorageImpl;
import com.bukhmastov.cdoitmo.util.impl.StorageLocalCacheImpl;
import com.bukhmastov.cdoitmo.util.impl.StoragePrefImpl;
import com.bukhmastov.cdoitmo.util.impl.ThemeImpl;
import com.bukhmastov.cdoitmo.util.impl.ThreadImpl;
import com.bukhmastov.cdoitmo.util.impl.TimeImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UtilsModule {

    @Provides
    @Singleton
    public Account provideAccount() {
        return new AccountImpl();
    }

    @Provides
    @Singleton
    public Accounts provideAccounts() {
        return new AccountsImpl();
    }

    @Provides
    @Singleton
    public Log provideLog() {
        return new LogImpl();
    }

    @Provides
    @Singleton
    public NavigationMenu provideNavigationMenu() {
        return new NavigationMenuImpl();
    }

    @Provides
    @Singleton
    public NotificationMessage provideNotificationMessage() {
        return new NotificationMessageImpl();
    }

    @Provides
    @Singleton
    public Notifications provideNotifications() {
        return new NotificationsImpl();
    }

    @Provides
    @Singleton
    public Static provideStatic() {
        return new StaticImpl();
    }

    @Provides
    @Singleton
    public Storage provideStorage() {
        return new StorageImpl();
    }

    @Provides
    @Singleton
    public StorageLocalCache provideStorageLocalCache() {
        return new StorageLocalCacheImpl();
    }

    @Provides
    @Singleton
    public StoragePref provideStoragePref() {
        return new StoragePrefImpl();
    }

    @Provides
    @Singleton
    public DateUtils provideDateUtils() {
        return new DateUtilsImpl();
    }

    @Provides
    @Singleton
    public Theme provideTheme() {
        return new ThemeImpl();
    }

    @Provides
    @Singleton
    public Thread provideThread() {
        return new ThreadImpl();
    }

    @Provides
    @Singleton
    public Time provideTime() {
        return new TimeImpl();
    }
}
