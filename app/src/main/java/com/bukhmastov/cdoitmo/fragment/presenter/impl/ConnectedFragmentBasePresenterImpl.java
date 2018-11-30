package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public abstract class ConnectedFragmentBasePresenterImpl {

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ConnectedFragmentBasePresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }
}
