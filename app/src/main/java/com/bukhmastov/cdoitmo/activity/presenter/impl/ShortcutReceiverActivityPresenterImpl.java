package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.ShortcutReceiverActivity;
import com.bukhmastov.cdoitmo.activity.presenter.ShortcutReceiverActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.receiver.ShortcutReceiver;
import com.bukhmastov.cdoitmo.util.Log;

import javax.inject.Inject;

public class ShortcutReceiverActivityPresenterImpl implements ShortcutReceiverActivityPresenter {

    private static final String TAG = "ShortcutReceiverActivity";
    private ShortcutReceiverActivity activity = null;

    @Inject
    Log log;

    public ShortcutReceiverActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull ShortcutReceiverActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        Intent remoteIntent = new Intent();
        remoteIntent.setAction(intent.getAction());
        if (extras != null) remoteIntent.putExtras(extras);
        log.v(TAG, "Activity created | action=" + remoteIntent.getAction() + " | " + remoteIntent.toString());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            new ShortcutReceiver().onReceive(activity, remoteIntent);
        } else {
            activity.sendBroadcast(remoteIntent);
        }
        activity.finish();
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Activity destroyed");
    }
}
