package com.bukhmastov.cdoitmo.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.bukhmastov.cdoitmo.utils.Wipe;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private final Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.i(TAG, "App | launched");
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Log.i(TAG, "App | version code = " + pInfo.versionCode);
            Log.i(TAG, "App | sdk = " + Build.VERSION.SDK_INT);
            Log.i(TAG, "App | theme = " + Static.getAppTheme(activity));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                // set default preferences
                PreferenceManager.setDefaultValues(activity, R.xml.pref_general, false);
                PreferenceManager.setDefaultValues(activity, R.xml.pref_cache, false);
                PreferenceManager.setDefaultValues(activity, R.xml.pref_notifications, false);
                PreferenceManager.setDefaultValues(activity, R.xml.pref_additional, false);
                // enable/disable firebase
                FirebaseCrashProvider.setEnabled(activity);
                FirebaseAnalyticsProvider.setEnabled(activity);
                // apply compatibility changes
                Wipe.check(activity);
                // init static variables
                Static.init(activity);
                // set auto_logout value
                LoginActivity.auto_logout = Storage.pref.get(activity, "pref_auto_logout", false);
                // set first_launch value
                Static.isFirstLaunchEver = Storage.pref.get(activity, "pref_first_launch", false);
                if (Static.isFirstLaunchEver) {
                    Storage.pref.put(activity, "pref_first_launch", false);
                }
                // firebase events and properties
                FirebaseAnalyticsProvider.logEvent(activity, FirebaseAnalyticsProvider.Event.APP_OPEN);
                FirebaseAnalyticsProvider.setUserProperty(activity, FirebaseAnalyticsProvider.Property.THEME, Static.getAppTheme(activity));
                // all done
                loaded();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    private void loaded() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                intent.setClass(activity, MainActivity.class);
                if (Intent.ACTION_MAIN.equals(intent.getAction())) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                startActivity(intent);
                if (Static.isFirstLaunchEver) {
                    startActivity(new Intent(activity, IntroducingActivity.class));
                }
                finish();
            }
        });
    }
}
