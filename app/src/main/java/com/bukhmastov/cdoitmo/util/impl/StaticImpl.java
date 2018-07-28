package com.bukhmastov.cdoitmo.util.impl;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.MainActivityEvent;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.UUID;

import javax.inject.Inject;

import dagger.Lazy;

public class StaticImpl implements Static {

    private static final String TAG = "Static";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    Lazy<Storage> storage;
    @Inject
    Lazy<Account> account;

    public StaticImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public String getUUID(Context context) {
        String uuid = storagePref.get().get(context, "pref_uuid", "");
        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
            storagePref.get().put(context, "pref_uuid", uuid);
        }
        return uuid;
    }

    @Override
    public void reLaunch(Context context) {
        log.i(TAG, "reLaunch");
        if (context == null) {
            log.w(TAG, "reLaunch | context is null");
            return;
        }
        eventBus.fire(new OpenActivityEvent(MainActivity.class, App.intentFlagRestart));
    }

    @Override
    public void hardReset(final Context context) {
        log.i(TAG, "hardReset");
        if (context == null) {
            log.w(TAG, "hardReset | context is null");
            return;
        }
        account.get().logoutPermanently(context, () -> {
            storage.get().clear(context, null);
            App.firstLaunch = true;
            App.OFFLINE_MODE = false;
            eventBus.fire(new MainActivityEvent.UnloadEvent());
            reLaunch(context);
        });
    }

    @Override
    public void lockOrientation(Activity activity, boolean lock) {
        try {
            if (activity != null) {
                log.v(TAG, "lockOrientation | activity=", activity.getComponentName().getClassName(), " | lock=", lock);
                activity.setRequestedOrientation(lock ? ActivityInfo.SCREEN_ORIENTATION_LOCKED : ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public void removeView(final View view) {
        thread.runOnUI(() -> {
            try {
                ((ViewGroup) view.getParent()).removeView(view);
            } catch (Throwable e) {
                log.exception(e);
            }
        });
    }
}
