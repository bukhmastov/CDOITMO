package com.bukhmastov.cdoitmo;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(this));
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_cache, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notifications, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_additional, false);
        Intent intent = new Intent(this, LoginActivity.class);
        Bundle extras = getIntent().getExtras();
        if (extras != null) intent.putExtras(extras);
        startActivity(intent);
        finish();
    }
}
