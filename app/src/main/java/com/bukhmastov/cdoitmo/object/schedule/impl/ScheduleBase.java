package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.network.IfmoClient;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.IsuPrivateRestClient;
import com.bukhmastov.cdoitmo.network.IsuRestClient;
import com.bukhmastov.cdoitmo.util.DateUtils;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

import dagger.Lazy;

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
    protected Lazy<IfmoRestClient> ifmoRestClient;
    @Inject
    protected Lazy<IfmoClient> ifmoClient;
    @Inject
    protected Lazy<IsuRestClient> isuRestClient;
    @Inject
    protected Lazy<IsuPrivateRestClient> isuPrivateRestClient;
    @Inject
    protected Time time;
    @Inject
    protected DateUtils dateUtils;
    @Inject
    protected FirebasePerformanceProvider firebasePerformanceProvider;

    ScheduleBase() {
        AppComponentProvider.getComponent().inject(this);
    }
}
