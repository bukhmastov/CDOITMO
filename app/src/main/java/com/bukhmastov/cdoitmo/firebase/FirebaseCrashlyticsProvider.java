package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.crashlytics.android.Crashlytics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.fabric.sdk.android.Fabric;

public class FirebaseCrashlyticsProvider {

    private static final String TAG = "FirebaseCrashlyticsProvider";
    private static boolean enabled = true;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({VERBOSE, DEBUG, INFO, WARN, ERROR})
    private @interface LEVEL {}
    private static final String VERBOSE = "VERBOSE";
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";

    public static boolean setEnabled(Context context) {
        return setEnabled(context, Storage.pref.get(context, "pref_allow_send_reports", true));
    }
    public static boolean setEnabled(Context context, boolean enabled) {
        try {
            FirebaseCrashlyticsProvider.enabled = enabled;
            if (FirebaseCrashlyticsProvider.enabled) {
                Fabric.with(context, new Crashlytics());
                Crashlytics.setUserIdentifier(Static.getUUID(context));
            }
            Log.i(TAG, "Firebase Crashlytics " + (FirebaseCrashlyticsProvider.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Static.error(e);
        }
        return FirebaseCrashlyticsProvider.enabled;
    }
    public static boolean setEnabled(ConnectedActivity activity, boolean enabled) {
        try {
            if (enabled) {
                FirebaseCrashlyticsProvider.enabled = true;
                Fabric.with(activity, new Crashlytics());
                Log.i(TAG, "Firebase Crashlytics enabled");
            } else {
                Static.snackBar(activity, activity.getString(R.string.changes_will_take_effect_next_startup));
                Log.i(TAG, "Firebase Crashlytics will be disabled at the next start up");
                FirebaseAnalyticsProvider.logBasicEvent(activity, "firebase_crash_disabled");
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return FirebaseCrashlyticsProvider.enabled;
    }

    public static void v(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(VERBOSE, TAG, log);
    }

    public static void d(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(DEBUG, TAG, log);
    }

    public static void i(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(INFO, TAG, log);
    }

    public static void w(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(WARN, TAG, log);
    }

    public static void e(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(ERROR, TAG, log);
    }

    public static void wtf(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.exception(new Exception("WTF" + "/" + TAG + " " + log));
    }

    public static void wtf(Throwable throwable) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.exception(throwable);
    }

    public static void exception(final Throwable throwable) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            try {
                if (!enabled) return;
                Crashlytics.logException(throwable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void log(final @LEVEL String level, final String TAG, final String log) {
        Static.T.runThread(Static.T.BACKGROUND, () -> {
            try {
                if (!enabled) return;
                Crashlytics.log(level2priority(level), TAG, log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static int level2priority(final @LEVEL String level) {
        switch(level) {
            default:
            case VERBOSE: return 2;
            case DEBUG: return 3;
            case INFO: return 4;
            case WARN: return 5;
            case ERROR: return 6;
        }
    }
}
