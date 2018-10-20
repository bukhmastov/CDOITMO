package com.bukhmastov.cdoitmo.model.parser;

import android.content.Context;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Time;

import javax.inject.Inject;

import dagger.Lazy;

public abstract class ParserBase {

    @Inject
    protected Log log;
    @Inject
    protected FirebasePerformanceProvider firebasePerformanceProvider;
    @Inject
    protected Context context;
    @Inject
    protected Lazy<Time> time;

    public ParserBase() {
        AppComponentProvider.getComponent().inject(this);
    }
}
