package com.bukhmastov.cdoitmo.model.converter;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

import dagger.Lazy;

public abstract class ConverterBase {

    @Inject
    protected Lazy<Log> log;
    @Inject
    protected Lazy<Storage> storage;
    @Inject
    protected Lazy<StoragePref> storagePref;
    @Inject
    protected Lazy<Time> time;
    @Inject
    protected Lazy<TextUtils> textUtils;
    @Inject
    protected Lazy<Context> context;
    @Inject
    protected Lazy<ScheduleLessonsHelper> scheduleLessonsHelper;
    @Inject
    protected FirebasePerformanceProvider firebasePerformanceProvider;

    ConverterBase() {
        AppComponentProvider.getComponent().inject(this);
    }
}
