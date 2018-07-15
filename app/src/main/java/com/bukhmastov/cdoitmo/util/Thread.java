package com.bukhmastov.cdoitmo.util;

import android.support.annotation.IntDef;

import com.bukhmastov.cdoitmo.util.impl.ThreadImpl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Thread {

    // future: replace with DI factory
    Thread instance = new ThreadImpl();
    static Thread instance() {
        return instance;
    }

    void run(final Runnable runnable);

    void run(final @TYPE int type, final Runnable runnable);

    void runOnUI(final Runnable runnable);

    void sleep(long millis) throws InterruptedException;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREGROUND, BACKGROUND})
    @interface TYPE {}
    int FOREGROUND = 0;
    int BACKGROUND = 1;
}
