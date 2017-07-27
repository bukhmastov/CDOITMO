package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;

import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.google.firebase.crash.FirebaseCrash;

public class FirebaseCrashProvider {

    private static final String TAG = "FirebaseCrashProvider";
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
        return setEnabled(context, enabled, false);
    }
    public static boolean setEnabled(Context context, boolean enabled, boolean notify) {
        try {
            if (!enabled && notify) {
                FirebaseAnalyticsProvider.logEvent(
                        context,
                        FirebaseAnalyticsProvider.Event.JOIN_GROUP,
                        FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.GROUP_ID, "crash_disabled")
                );
            }
            FirebaseCrashProvider.enabled = enabled;
            FirebaseCrash.setCrashCollectionEnabled(FirebaseCrashProvider.enabled);
            Log.i(TAG, "Firebase Crash " + (FirebaseCrashProvider.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            Static.error(e);
        }
        return FirebaseCrashProvider.enabled;
    }

    public static void v(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.log(LEVEL.VERBOSE, TAG, log);
    }

    public static void d(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.log(LEVEL.DEBUG, TAG, log);
    }

    public static void i(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.log(LEVEL.INFO, TAG, log);
    }

    public static void w(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.log(LEVEL.WARN, TAG, log);
    }

    public static void e(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.log(LEVEL.ERROR, TAG, log);
    }

    public static void wtf(String TAG, String log) {
        if (!enabled) return;
        FirebaseCrashProvider.exception(new Exception("WTF" + "/" + TAG + " " + log));
    }

    public static void wtf(Throwable throwable) {
        if (!enabled) return;
        FirebaseCrashProvider.exception(throwable);
    }

    public static void exception(final Throwable throwable) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    FirebaseCrash.report(throwable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void log(final LEVEL level, final String TAG, final String log) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                try {
                    if (!enabled) return;
                    FirebaseCrash.log(level2string(level) + "/" + TAG + " " + log);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
