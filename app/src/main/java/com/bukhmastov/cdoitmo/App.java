package com.bukhmastov.cdoitmo;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;

import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.singleton.TextUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Roadmap 2k18
 *
 * 1. Подготовка к DI: абстракция (TODO interface - impl):
 *  storage [done]
 *  network [done]
 *  firebase [done]
 *  log [done]
 *  thread [done]
 *  objects
 *  utils
 *
 * 2. Избавление от оставшихся статичных методов и полей
 *
 * 3. Добавление объектов данных / оставление все в json представлениях
 *
 * 4. DI:
 *  app
 *  activity
 *  fragment
 *  view/widget
 *  storage
 *  network
 *  firebase
 *  objects
 *  utils
 *
 * ???
 *
 * 5. Профит (Столько не живут)
 *
 * 6. ИСУ (попытка номер два)
 *
 */
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

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();
    //@Inject
    private FirebaseCrashlyticsProvider firebaseCrashlyticsProvider = FirebaseCrashlyticsProvider.instance();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            log.setEnabled(storagePref.get(this, "pref_allow_collect_logs", false));
            locale = TextUtils.getLocale(this, storagePref);
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
