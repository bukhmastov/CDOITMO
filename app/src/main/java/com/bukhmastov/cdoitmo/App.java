package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.bus.annotation.Event;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;

import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

public class App extends Application {

    private static final String TAG = "Application";
    public static final int intentFlagRestart = Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK;
    public static String versionName;
    public static int versionCode;
    public static boolean OFFLINE_MODE = false;
    public static boolean UNAUTHORIZED_MODE = false;
    public static boolean firstLaunch = true;
    public static boolean tablet = false;
    public static boolean isFirstLaunchEver = false;
    public static boolean showIntroducingActivity = false;
    private Locale locale;

    @Inject
    Log log;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;
    @Inject
    FirebaseCrashlyticsProvider firebaseCrashlyticsProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            AppComponentProvider.getComponent().inject(this);
            eventBus.register(this);
            log.setEnabled(storagePref.get(this, "pref_allow_collect_logs", false));
            locale = textUtils.getLocale(this, storagePref);
            log.i(TAG, "Language | locale=" + locale.toString());
            init();
            setUUID();
            setLocale();
            setFirebase();
        } catch (Throwable e) {
            log.exception(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            setLocale();
        } catch (Throwable e) {
            log.exception(e);
        }
    }

    @Event
    public void onOpenActivityEvent(OpenActivityEvent event) {
        try {
            log.v(TAG, "onOpenActivityEvent | activity=", event.getActivity().getCanonicalName());
            Intent intent = new Intent(this, event.getActivity());
            if (event.getExtras() != null) {
                intent.putExtras(event.getExtras());
            }
            if (event.getFlags() != null) {
                intent.addFlags(event.getFlags());
            }
            startActivity(intent);
        } catch (Throwable t) {
            log.e(TAG, "Failed to open activity | throwable=", t.getMessage());
        }
    }

    private void init() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            App.versionName = pInfo.versionName;
            App.versionCode = pInfo.versionCode;
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private void setUUID() {
        if (!storagePref.exists(this, "pref_uuid")) {
            storagePref.put(this, "pref_uuid", UUID.randomUUID().toString());
        }
    }

    private void setLocale() {
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void setFirebase() {
        firebaseCrashlyticsProvider.setEnabled(this);
        firebaseAnalyticsProvider.setEnabled(this);
    }
}
