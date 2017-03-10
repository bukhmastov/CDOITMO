package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.Cache;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.Map;
import java.util.regex.Pattern;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Static.darkTheme = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_theme", false);
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("pref_auto_logout", false)) Static.logout(this);

        loaded();
    }

    private static class Wipe {
        private static final int number = 2;
        static void check(Context context) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean("wipe_" + number, true)) {
                sharedPreferences.edit().remove("wipe_" + (number - 1)).putBoolean("wipe_" + number, false).apply();
                apply(context);
            }
        }
        private static void apply(Context context) {
            Cache.clear(context);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> list = sharedPreferences.getAll();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("ProtocolTrackerHISTORY");
            for(Map.Entry<String, ?> entry : list.entrySet()) {
                if(Pattern.compile("^widget_\\d+_cache.*").matcher(entry.getKey()).find()) editor.remove(entry.getKey());
            }
            editor.apply();
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