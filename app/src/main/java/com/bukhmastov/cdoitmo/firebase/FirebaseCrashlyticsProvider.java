package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class FirebaseCrashlyticsProvider {

    private static final String TAG = "FirebaseCrashlyticsProvider";
    private static boolean enabled = true;
    private enum LEVEL {VERBOSE, DEBUG, INFO, WARN, ERROR}

    private static String level2string(LEVEL level) {
        switch (level) {
            case ERROR: return "ERROR";
            case WARN: return "WARN";
            case INFO: return "INFO";
            case DEBUG: return "DEBUG";
            case VERBOSE: default: return "VERBOSE";
        }
    }

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
                FirebaseAnalyticsProvider.logEvent(
                        activity,
                        FirebaseAnalyticsProvider.Event.JOIN_GROUP,
                        FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.GROUP_ID, "crash_disabled")
                );
            }
        } catch (Exception e) {
            Static.error(e);
        }
        return FirebaseCrashlyticsProvider.enabled;
    }

    public static void v(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(LEVEL.VERBOSE, TAG, log);
    }

    public static void d(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(LEVEL.DEBUG, TAG, log);
    }

    public static void i(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(LEVEL.INFO, TAG, log);
    }

    public static void w(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(LEVEL.WARN, TAG, log);
    }

    public static void e(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashlyticsProvider.log(LEVEL.ERROR, TAG, log);
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
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            try {
                if (!enabled) return;
                Crashlytics.logException(throwable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void log(final LEVEL level, final String TAG, final String log) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            try {
                if (!enabled) return;
                Crashlytics.log(level2string(level) + "/" + TAG + " " + log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
