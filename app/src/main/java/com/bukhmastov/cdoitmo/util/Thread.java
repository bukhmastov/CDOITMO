package com.bukhmastov.cdoitmo.util;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Thread {

    private static final String TAG = "Thread";
    private static final boolean DEBUG = false;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOREGROUND, BACKGROUND})
    public @interface TYPE {}
    public static final int FOREGROUND = 0;
    public static final int BACKGROUND = 1;

    public static void run(final Runnable runnable) {
        run(FOREGROUND, runnable);
    }

    public static void run(final @TYPE int type, final Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("Passed runnable is null");
        }
        final HasThread hasThread = getThread(type);
        if (hasThread.thread != null && !hasThread.thread.isAlive()) {
            log("run | HandlerThread is not alive, going to quit | id = " + hasThread.thread.getId() + " | name = " + hasThread.thread.getName());
            Looper looper = hasThread.thread.getLooper();
            if (looper != null) {
                looper.quit();
            }
            try {
                hasThread.thread.interrupt();
            } catch (Throwable ignore) {
                // just ignore
            }
            hasThread.thread = null;
        }
        if (hasThread.thread == null) {
            hasThread.thread = new HandlerThread(hasThread.thread_name, hasThread.thread_priority);
            hasThread.thread.start();
            log("run | initialized new HandlerThread | id = " + hasThread.thread.getId() + " | name = " + hasThread.thread.getName());
        }
        log("run | run with Handler.post | id = " + hasThread.thread.getId() + " | name = " + hasThread.thread.getName());
        try {
            new Handler(hasThread.thread.getLooper()).post(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    Log.exception("Run on " + hasThread.thread.getName() + " thread failed", throwable);
                }
            });
        } catch (Throwable throwable) {
            Log.exception("Run on " + hasThread.thread.getName() + " thread failed", throwable);
        }
    }

    public static void runOnUI(final Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("Passed runnable is null");
        }
        if (isMainThread()) {
            log("runOnUI | run on current thread");
            try {
                runnable.run();
            } catch (Throwable throwable) {
                Log.exception("Run on main thread failed", throwable);
            }
        } else {
            log("runOnUI | run with Handler.post");
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable throwable) {
                        Log.exception("Run on main thread failed", throwable);
                    }
                });
            } catch (Throwable throwable) {
                Log.exception("Run on main thread failed", throwable);
            }
        }
    }

    public static void sleep(long millis) throws InterruptedException {
        java.lang.Thread.sleep(millis, 0);
    }

    private static boolean isMainThread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : java.lang.Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private static boolean isLooperThread() {
        return java.lang.Thread.currentThread() == Foreground.thread || java.lang.Thread.currentThread() == Background.thread || isMainThread();
    }

    private static HasThread getThread(@TYPE int type) {
        switch (type) {
            case BACKGROUND: return Background;
            case FOREGROUND: default: return Foreground;
        }
    }

    private static void log(String log) {
        if (DEBUG) {
            android.util.Log.v(TAG, log);
        }
    }

    private static class HasThread {
        public final @TYPE
        int type;
        public HandlerThread thread = null;
        public final String thread_name;
        public final int thread_priority;
        public HasThread(@TYPE int type, String thread_name, int thread_priority) {
            this.type = type;
            this.thread_name = thread_name;
            this.thread_priority = thread_priority;
        }
    }
    private static final HasThread Foreground = new HasThread(FOREGROUND, "CDOExecutorForeground", Process.THREAD_PRIORITY_FOREGROUND);
    private static final HasThread Background = new HasThread(BACKGROUND, "CDOExecutorBackground", Process.THREAD_PRIORITY_BACKGROUND);
}
