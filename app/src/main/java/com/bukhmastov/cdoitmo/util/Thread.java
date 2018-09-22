package com.bukhmastov.cdoitmo.util;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Thread {

    void run(final Runnable runnable);

    void run(final @TYPE int type, final Runnable runnable);

    void runOnUI(final Runnable runnable);

    void sleep(long millis) throws InterruptedException;

    boolean assertUI();

    boolean assertNotUI();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREGROUND, BACKGROUND})
    @interface TYPE {}
    int FOREGROUND = 0;
    int BACKGROUND = 1;
}
