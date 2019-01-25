package com.bukhmastov.cdoitmo.util;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Thread {

    void run(@NonNull ThrowingRunnable runnable);

    void run(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void run(@TYPE int type, @NonNull ThrowingRunnable runnable);

    void run(@TYPE int type, @NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void runOnUI(@NonNull ThrowingRunnable runnable);

    void runOnUI(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void sleep(long millis) throws InterruptedException;

    boolean assertUI();

    boolean assertNotUI();

    /**
     * Will crash the application
     * Only for debugging purposes
     * Will not take affect if debug mode turned off
     * @see com.bukhmastov.cdoitmo.App.DEBUG
     */
    void uncaught(Throwable throwable);

    void initUncaughtExceptionHandler();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREGROUND, BACKGROUND})
    @interface TYPE {}
    int FOREGROUND = 0;
    int BACKGROUND = 1;
}
