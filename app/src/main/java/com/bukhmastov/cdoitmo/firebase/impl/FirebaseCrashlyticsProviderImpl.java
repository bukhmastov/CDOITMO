package com.bukhmastov.cdoitmo.firebase.impl;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseCrashlyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.crashlytics.android.Crashlytics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import androidx.annotation.StringDef;
import dagger.Lazy;
import io.fabric.sdk.android.Fabric;

public class FirebaseCrashlyticsProviderImpl implements FirebaseCrashlyticsProvider {

    private static final String TAG = "FirebaseCrashlyticsProvider";
    private boolean enabled = true;

    @Inject
    Log log;
    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    Lazy<NotificationMessage> notificationMessage;
    @Inject
    Lazy<Static> staticUtil;
    @Inject
    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({VERBOSE, DEBUG, INFO, WARN, ERROR})
    private @interface LEVEL {}
    private static final String VERBOSE = "VERBOSE";
    private static final String DEBUG = "DEBUG";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";

    public FirebaseCrashlyticsProviderImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public boolean setEnabled(Context context) {
        return setEnabled(context, storagePref.get().get(context, "pref_allow_send_reports", true));
    }

    @Override
    public boolean setEnabled(Context context, boolean enabled) {
        try {
            this.enabled = enabled;
            if (this.enabled) {
                Fabric.with(context, new Crashlytics());
                Crashlytics.setUserIdentifier(staticUtil.get().getUUID(context));
            }
            log.i(TAG, "Firebase Crashlytics " + (this.enabled ? "enabled" : "disabled"));
        } catch (Exception e) {
            log.exception(e);
        }
        return this.enabled;
    }

    @Override
    public boolean setEnabled(ConnectedActivity activity, boolean enabled) {
        try {
            if (enabled) {
                this.enabled = true;
                Fabric.with(activity, new Crashlytics());
                log.i(TAG, "Firebase Crashlytics enabled");
            } else {
                notificationMessage.get().snackBar(activity, activity.getString(R.string.changes_will_take_effect_next_startup));
                log.i(TAG, "Firebase Crashlytics will be disabled at the next start up");
                firebaseAnalyticsProvider.get().logBasicEvent(activity, "firebase_crash_disabled");
            }
        } catch (Exception e) {
            log.exception(e);
        }
        return this.enabled;
    }

    @Override
    public void v(String TAG, String log) {
        if (!enabled) return;
        log(VERBOSE, TAG, log);
    }

    @Override
    public void d(String TAG, String log) {
        if (!enabled) return;
        log(DEBUG, TAG, log);
    }

    @Override
    public void i(String TAG, String log) {
        if (!enabled) return;
        log(INFO, TAG, log);
    }

    @Override
    public void w(String TAG, String log) {
        if (!enabled) return;
        log(WARN, TAG, log);
    }

    @Override
    public void e(String TAG, String log) {
        if (!enabled) return;
        log(ERROR, TAG, log);
    }

    @Override
    public void wtf(String TAG, String log) {
        if (!enabled) return;
        exception(new Exception("WTF" + "/" + TAG + " " + log));
    }

    @Override
    public void wtf(Throwable throwable) {
        if (!enabled) return;
        exception(throwable);
    }

    @Override
    public void exception(Throwable throwable) {
        if (!enabled) return;
        try {
            Crashlytics.logException(throwable);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        // thread.standalone(() -> Crashlytics.logException(throwable), Throwable::printStackTrace);
    }

    private void log(@LEVEL String level, String TAG, String log) {
        if (!enabled) return;
        try {
            Crashlytics.log(level2priority(level), TAG, log);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        //thread.standalone(() -> Crashlytics.log(level2priority(level), TAG, log), Throwable::printStackTrace);
    }

    private int level2priority(final @LEVEL String level) {
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
