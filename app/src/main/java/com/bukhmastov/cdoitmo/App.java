package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import java.util.Locale;

/* TODO
 * дропдауны
 */
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

    private void setLocale() throws Throwable {
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void setFirebase() {
        FirebaseCrashProvider.setEnabled(this);
        FirebaseAnalyticsProvider.setEnabled(this);
    }
}
