package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;

import java.util.Locale;
import java.util.UUID;

public class App extends Application {

    private static final String TAG = "Application";
    private Locale locale;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.setEnabled(Storage.pref.get(this, "pref_allow_collect_logs", false));
            locale = Static.getLocale(this);
            Log.i(TAG, "Language | locale=" + locale.toString());
            setUUID();
            setLocale();
            setFirebase();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            setLocale();
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    private void setUUID() {
        if (!Storage.pref.exists(this, "pref_uuid")) {
            Storage.pref.put(this, "pref_uuid", UUID.randomUUID().toString());
        }
    }

    private void setLocale() throws Throwable {
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
