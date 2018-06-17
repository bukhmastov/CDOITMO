package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;

import java.util.Locale;
import java.util.UUID;

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

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.setEnabled(Storage.pref.get(this, "pref_allow_collect_logs", false));
            locale = TextUtils.getLocale(this);
            Log.i(TAG, "Language | locale=" + locale.toString());
            init();
            setUUID();
            setLocale();
            setFirebase();
        } catch (Throwable e) {
            Log.exception(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            setLocale();
        } catch (Throwable e) {
            Log.exception(e);
        }
    }

    private void init() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            App.versionName = pInfo.versionName;
            App.versionCode = pInfo.versionCode;
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    private void setUUID() {
        if (!Storage.pref.exists(this, "pref_uuid")) {
            Storage.pref.put(this, "pref_uuid", UUID.randomUUID().toString());
        }
    }

    private void setLocale() {
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void setFirebase() {
        FirebaseCrashlyticsProvider.setEnabled(this);
        FirebaseAnalyticsProvider.setEnabled(this);
    }
}
