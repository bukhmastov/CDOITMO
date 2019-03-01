package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Thread;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

public abstract class ConnectedFragmentPresenterImpl extends ConnectedFragmentBasePresenterImpl
        implements ConnectedFragmentPresenter {

    protected ConnectedFragment fragment = null;
    protected ConnectedActivity activity = null;
    protected Client.Request requestHandle = null;
    protected boolean loaded = false;
    protected boolean forbidden = false;

    public ConnectedFragmentPresenterImpl() {
        super();
    }

    protected abstract String getLogTag();
    protected @Thread.ThreadToken String getThreadToken() {
        return null;
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(getLogTag(), "Fragment created");
        if (getThreadToken() != null) {
            thread.initialize(getThreadToken());
        }
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    @CallSuper
    public void onViewCreated() {
        if (getThreadToken() != null) {
            thread.initialize(getThreadToken());
        }
    }

    @Override
    public void onDestroy() {
        log.v(getLogTag(), "Fragment destroyed");
        if (getThreadToken() != null) {
            thread.interrupt(getThreadToken());
        }
        loaded = false;
    }

    @Override
    public void onResume() {
        log.v(getLogTag(), "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        log.v(getLogTag(), "Fragment paused");
        thread.standalone(() -> {
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }
}
