package com.bukhmastov.cdoitmo.util;

import androidx.annotation.NonNull;

public interface Log {

    void setEnabled(boolean enabled);

    int v(String TAG, Object... log);

    int d(Object... log);

    int i(String TAG, Object... log);

    int w(String TAG, Object... log);

    int e(String TAG, Object... log);

    int wtf(String TAG, Object... log);

    int wtf(Throwable throwable);

    int exception(Throwable throwable);

    int exception(Throwable throwable, Object... log);

    int exception(String msg, Throwable throwable);

    @NonNull
    String getLog();

    @NonNull
    String getLog(boolean reverse);
}
