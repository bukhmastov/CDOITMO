package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkedAccountsFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class LinkedAccountsFragmentPresenterImpl implements LinkedAccountsFragmentPresenter {

    private static final String TAG = "LinkedAccountsFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    
    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Storage storage;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    
    public LinkedAccountsFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
    }

    @Override
    public void onViewCreated() {
        try {
            final View account_cdo_link = fragment.container().findViewById(R.id.account_cdo_link);
            final View account_cdo_info = fragment.container().findViewById(R.id.account_cdo_info);
            if (account_cdo_link != null) {
                account_cdo_link.setOnClickListener(v -> {
                    log.v(TAG, "account_cdo_link clicked");
                    eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://de.ifmo.ru"))));
                });
            }
            thread.run(() -> {
                final String cdo_user_info = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim() + " (" + storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name", "").trim() + ")";
                thread.runOnUI(() -> {
                    if (account_cdo_info != null) {
                        ((TextView) account_cdo_info).setText(cdo_user_info);
                    }
                });
            });
        } catch (Exception e) {
            log.exception(e);
        }
    }
}
