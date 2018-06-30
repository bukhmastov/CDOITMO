package com.bukhmastov.cdoitmo.firebase;

import android.content.Context;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseCrashlyticsProviderImpl;

public interface FirebaseCrashlyticsProvider {

    // future: replace with DI factory
    FirebaseCrashlyticsProvider instance = new FirebaseCrashlyticsProviderImpl();
    static FirebaseCrashlyticsProvider instance() {
        return instance;
    }

    boolean setEnabled(Context context);
    boolean setEnabled(Context context, boolean enabled);
    boolean setEnabled(ConnectedActivity activity, boolean enabled);

    void v(String TAG, String log);

    void d(String TAG, String log);

    void i(String TAG, String log);

    void w(String TAG, String log);

    void e(String TAG, String log);

    void wtf(String TAG, String log);

    void wtf(Throwable throwable);

    void exception(final Throwable throwable);
}
