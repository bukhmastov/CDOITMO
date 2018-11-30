package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.network.model.Client;

import androidx.annotation.Nullable;

public abstract class ConnectedFragmentPresenterImpl extends ConnectedFragmentBasePresenterImpl
        implements ConnectedFragmentPresenter {

    protected ConnectedFragment fragment = null;
    protected ConnectedActivity activity = null;
    protected Client.Request requestHandle = null;
    protected boolean loaded = false;

    public ConnectedFragmentPresenterImpl() {
        super();
    }

    protected abstract String getLogTag();

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.run(() -> {
            log.v(getLogTag(), "Fragment created");
            firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        });
    }

    @Override
    public void onDestroy() {
        log.v(getLogTag(), "Fragment destroyed");
        loaded = false;
    }

    @Override
    public void onResume() {
        thread.run(() -> {
            log.v(getLogTag(), "Fragment resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        });
    }

    @Override
    public void onPause() {
        thread.run(() -> {
            log.v(getLogTag(), "Fragment paused");
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }
}
