package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ConnectedFragmentPresenter;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Thread;

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
        if (getThreadToken() != null) {
            thread.initialize(getThreadToken());
        }
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onViewCreated() {
        if (getThreadToken() != null) {
            thread.initialize(getThreadToken());
        }
    }

    @Override
    public void onStart() {}

    @Override
    public void onResume() {
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        thread.standalone(() -> {
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onStop() {}

    @Override
    public void onDestroy() {
        if (getThreadToken() != null) {
            thread.interrupt(getThreadToken());
        }
        loaded = false;
    }
}
