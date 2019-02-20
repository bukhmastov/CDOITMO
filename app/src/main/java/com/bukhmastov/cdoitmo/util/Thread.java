package com.bukhmastov.cdoitmo.util;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * App has multiple threads, but performs tasks consistently task by task.
 * All code-managed app threads divided to 4 groups:
 * 1. Main thread (UI)
 *    Should contain only tasks, that going to update user interface.
 *    All tasks performed consistently at one looper thread.
 * 2. Background thread, that important for UI (FOREGROUND)
 *    Should contain tasks, that preparing data for user interface.
 *    All tasks performed consistently at one looper thread.
 * 3. Background thread, that not important for UI (BACKGROUND)
 *    Should contain tasks, that not really important for user interface. Purpose of this group
 *    is to create second background queue, that not blocking tasks, that important for UI.
 *    All tasks performed consistently at one looper thread.
 * 4. Standalone thread, that do heavy work (STANDALONE)
 *    Tasks, that do heavy work and not required to be in queue.
 *    Tasks cannot know which state the application will be at the end of the task.
 *    Each task performed at standalone thread.
 */
public interface Thread {

    void run(@NonNull ThrowingRunnable runnable);

    void run(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void run(@TYPE int type, @NonNull ThrowingRunnable runnable);

    void run(@TYPE int type, @NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void runOnUI(@NonNull ThrowingRunnable runnable);

    void runOnUI(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void standalone(@NonNull ThrowingRunnable runnable);

    void standalone(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

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
