package com.bukhmastov.cdoitmo.activities;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.darkTheme = Storage.pref.get(this, "pref_dark_theme", false);
        super.onCreate(savedInstanceState);
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

        if (Storage.pref.get(this, "pref_auto_logout", false)) Static.logout(this);

        loaded();
    }

    private static class Wipe {
        static void check(Context context) {
            try {
                int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                if (Storage.pref.get(context, "last_version", 0) < versionCode) {
                    apply(context, versionCode);
                    Storage.pref.put(context, "last_version", versionCode);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Static.error(e);
            }
        }
        private static void apply(Context context, int versionCode) {
            if (versionCode == 25) {
                ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
                Static.logout(context);
                Storage.pref.clearExceptPref(context);
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