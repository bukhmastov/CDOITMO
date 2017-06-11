package com.bukhmastov.cdoitmo.activities;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.darkTheme = Storage.pref.get(this, "pref_dark_theme", false);
        super.onCreate(savedInstanceState);
        try {
            Log.i(TAG, "App | launched");
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Log.i(TAG, "App | version code = " + pInfo.versionCode);
            Log.i(TAG, "App | sdk = " + Build.VERSION.SDK_INT);
            Log.i(TAG, "App | dark theme = " + (Storage.pref.get(this, "pref_dark_theme", false) ? "true" : "false"));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_cache, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_additional, false);

        Wipe.check(this);

        Static.init(this);

        LoginActivity.auto_logout = Storage.pref.get(this, "pref_auto_logout", false);

        loaded();
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

    private static class Wipe {
        static void check(Context context) {
            try {
                int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                int lastVersionCode = Storage.pref.get(context, "last_version", 0);
                if (lastVersionCode < versionCode) {
                    for (int i = lastVersionCode + 1; i <= versionCode; i++) {
                        apply(context, i);
                    }
                    Storage.pref.put(context, "last_version", versionCode);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Static.error(e);
            }
        }
        private static void apply(Context context, int versionCode) {
            Log.i(TAG, "Wipe apply for versionCode " + versionCode);
            switch (versionCode) {
                case 26: {
                    ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
                    Static.logout(context);
                    Storage.pref.clearExceptPref(context);
                    break;
                }
                case 29: {
                    Storage.file.cache.delete(context, "eregister#core");
                    break;
                }
                case 51: {
                    Storage.file.cache.clear(context, "protocol#log");
                    if (Storage.pref.get(context, "pref_protocol_changes_track", true)) {
                        Static.protocolChangesTrackSetup(context, 0);
                    }
                    break;
                }
            }
        }
    }

    private void loaded() {
        Intent intent = new Intent(this, MainActivity.class);
        Bundle extras = getIntent().getExtras();
        if (extras != null) intent.putExtras(extras);
        startActivity(intent);
        finish();
    }

}