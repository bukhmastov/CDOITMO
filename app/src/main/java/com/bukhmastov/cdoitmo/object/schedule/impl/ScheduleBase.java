package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.provider.StorageProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

public abstract class ScheduleBase {

    @Inject
    protected Log log;
    @Inject
    protected Thread thread;
    @Inject
    protected Context context;
    @Inject
    protected EventBus eventBus;
    @Inject
    protected Storage storage;
    @Inject
    protected StoragePref storagePref;
    @Inject
    protected StorageProvider storageProvider;
    @Inject
    protected IfmoRestClient ifmoRestClient;
    @Inject
    protected IfmoClient ifmoClient;
    @Inject
    protected Time time;
    @Inject
    protected TextUtils textUtils;
    @Inject
    protected FirebasePerformanceProvider firebasePerformanceProvider;

    ScheduleBase() {
        AppComponentProvider.getComponent().inject(this);
    }
}
