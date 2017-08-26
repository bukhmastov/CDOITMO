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
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseMessagingProvider;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.ProtocolTracker;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONObject;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private SplashActivity self = this;

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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                // set default preferences
                PreferenceManager.setDefaultValues(self, R.xml.pref_general, false);
                PreferenceManager.setDefaultValues(self, R.xml.pref_cache, false);
                PreferenceManager.setDefaultValues(self, R.xml.pref_notifications, false);
                PreferenceManager.setDefaultValues(self, R.xml.pref_additional, false);
                // enable/disable firebase
                FirebaseCrashProvider.setEnabled(self);
                FirebaseAnalyticsProvider.setEnabled(self);
                // apply compatibility changes
                Wipe.check(self);
                // init static variables
                Static.init(self);
                // set auto_logout value
                LoginActivity.auto_logout = Storage.pref.get(self, "pref_auto_logout", false);
                // set first_launch value
                Static.isFirstLaunchEver = Storage.pref.get(self, "pref_first_launch", false);
                if (Static.isFirstLaunchEver) {
                    Storage.pref.put(self, "pref_first_launch", false);
                }
                // firebase events and properties
                FirebaseAnalyticsProvider.logEvent(self, FirebaseAnalyticsProvider.Event.APP_OPEN);
                FirebaseAnalyticsProvider.setUserProperty(self, FirebaseAnalyticsProvider.Property.THEME, Storage.pref.get(self, "pref_dark_theme", false) ? "dark" : "light");
                // all done
                loaded();
            }
        });
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
                case 58: {
                    FirebaseMessagingProvider.checkOwnerNotification(context);
                    break;
                }
                case 62: {
                    Storage.pref.put(context, "pref_dynamic_refresh", Storage.pref.get(context, "pref_tab_refresh", "0"));
                    Storage.pref.put(context, "pref_static_refresh", Storage.pref.get(context, "pref_schedule_refresh", "168"));
                    Storage.pref.delete(context, "pref_tab_refresh");
                    Storage.pref.delete(context, "pref_schedule_refresh");
                    break;
                }
                case 71: {
                    Storage.pref.delete(context, "pref_open_drawer_at_startup");
                    Storage.pref.put(context, "pref_first_launch", Storage.file.general.get(context, "users#list", "").trim().isEmpty());
                    new ProtocolTracker(context).reset();
                    break;
                }
                case 78: {
                    ArrayList<String> appWidgetIds = Storage.file.general.list(context, "widget_schedule_lessons");
                    for (String appWidgetId : appWidgetIds) {
                        String settings = Storage.file.general.get(context, "widget_schedule_lessons#" + appWidgetId + "#settings", "");
                        boolean empty_settings = false;
                        if (settings != null) {
                            settings = settings.trim();
                            if (!settings.isEmpty()) {
                                try {
                                    JSONObject settingsJson = new JSONObject(settings);
                                    if (settingsJson.has("darkTheme")) {
                                        boolean darkTheme = settingsJson.getBoolean("darkTheme");
                                        JSONObject theme = new JSONObject();
                                        if (darkTheme) {
                                            theme.put("background", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.background);
                                            theme.put("text", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.text);
                                            theme.put("opacity", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.opacity);
                                        } else {
                                            theme.put("background", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.background);
                                            theme.put("text", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.text);
                                            theme.put("opacity", ScheduleLessonsWidgetConfigureActivity.Default.Theme.Light.opacity);
                                        }
                                        settingsJson.remove("darkTheme");
                                        settingsJson.put("theme", theme);
                                        Storage.file.general.put(context, "widget_schedule_lessons#" + appWidgetId + "#settings", settingsJson.toString());
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                    Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#settings");
                                }
                            } else {
                                empty_settings = true;
                            }
                        } else {
                            empty_settings = true;
                        }
                        if (empty_settings) {
                            Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#settings");
                            Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#cache");
                            Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#cache_converted");
                        }
                    }
                    break;
                }
            }
        }
    }

    private void loaded() {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(self, MainActivity.class);
                Bundle extras = getIntent().getExtras();
                if (extras != null) intent.putExtras(extras);
                startActivity(intent);
                if (Static.isFirstLaunchEver) {
                    startActivity(new Intent(self, IntroducingActivity.class));
                }
                finish();
            }
        });
    }
}
