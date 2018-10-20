package com.bukhmastov.cdoitmo.object.impl;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public abstract class SettingsScheduleBase {

    @Inject
    protected Log log;
    @Inject
    protected Thread thread;
    @Inject
    protected StoragePref storagePref;
    @Inject
    protected NotificationMessage notificationMessage;

    SettingsScheduleBase() {
        AppComponentProvider.getComponent().inject(this);
    }
}
