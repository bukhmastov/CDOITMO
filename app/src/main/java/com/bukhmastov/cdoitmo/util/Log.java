package com.bukhmastov.cdoitmo.util;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.util.impl.LogImpl;

public interface Log {

    // future: replace with DI factory
    Log instance = new LogImpl();
    static Log instance() {
        return instance;
    }

    void setEnabled(boolean enabled);

    int v(String TAG, Object... log);

    int d(Object... log);

    int i(String TAG, Object... log);

    int w(String TAG, Object... log);

    int e(String TAG, Object... log);

    int wtf(String TAG, Object... log);

    int wtf(Throwable throwable);

    int exception(Throwable throwable);

    int exception(String msg, Throwable throwable);

    @NonNull
    String getLog();

    @NonNull
    String getLog(boolean reverse);
}
