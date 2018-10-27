package com.bukhmastov.cdoitmo.util.impl;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

import dagger.Lazy;

public class ThreadImpl implements Thread {

    private static final String TAG = "Thread";
    private static final boolean DEBUG = App.DEBUG;

    @Inject
    Lazy<Log> log;
    @Inject
    Lazy<Context> context;

    public ThreadImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void run(@NonNull ThrowingRunnable runnable) {
        run(FOREGROUND, runnable);
    }

    @Override
    public void run(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        run(FOREGROUND, runnable, errorHandler);
    }

    @Override
    public void run(@Thread.TYPE int type, final @NonNull ThrowingRunnable runnable) {
        run(FOREGROUND, runnable, null);
    }

    @Override
    public void run(@Thread.TYPE int type, @NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        final ThreadFacade threadFacade = getThreadFacade(type);
        if (threadFacade.thread != null && !threadFacade.thread.isAlive()) {
            log("run | HandlerThread is not alive, going to quit | id = " + threadFacade.thread.getId() + " | name = " + threadFacade.thread.getName());
            Looper looper = threadFacade.thread.getLooper();
            if (looper != null) {
                looper.quit();
            }
            try {
                threadFacade.thread.interrupt();
            } catch (Throwable ignore) {
                // just ignore
            }
            threadFacade.thread = null;
        }
        if (threadFacade.thread == null) {
            threadFacade.thread = new HandlerThread(threadFacade.name, threadFacade.priority);
            threadFacade.thread.start();
            log("run | initialized new HandlerThread | id = " + threadFacade.thread.getId() + " | name = " + threadFacade.thread.getName());
        }
        // log("run | run with Handler.post | id = " + threadFacade.thread.getId() + " | name = " + threadFacade.thread.getName());
        try {
            new Handler(threadFacade.thread.getLooper()).post(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    onCaughtUncaughtException(threadFacade.thread.getName(), throwable, errorHandler);
                }
            });
        } catch (Throwable throwable) {
            onCaughtUncaughtException(threadFacade.thread.getName(), throwable, errorHandler);
        }
    }

    @Override
    public void runOnUI(@NonNull ThrowingRunnable runnable) {
        runOnUI(runnable, null);
    }

    @Override
    public void runOnUI(@NonNull ThrowingRunnable runnable, @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        if (isMainThread()) {
            // log("runOnUI | run on current thread");
            try {
                runnable.run();
            } catch (Throwable throwable) {
                onCaughtUncaughtException("main", throwable, errorHandler);
            }
        } else {
            // log("runOnUI | run with Handler.post");
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        runnable.run();
                    } catch (Throwable throwable) {
                        onCaughtUncaughtException("main", throwable, errorHandler);
                    }
                });
            } catch (Throwable throwable) {
                onCaughtUncaughtException("main", throwable, errorHandler);
            }
        }
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        java.lang.Thread.sleep(millis, 0);
    }

    @Override
    public boolean assertUI() {
        if (isMainThread()) {
            return true;
        }
        if (App.DEBUG) {
            uncaught(new IllegalStateException("Main thread required"));
        } else {
            log.get().wtf(new IllegalStateException("Main thread required"));
        }
        return false;
    }

    @Override
    public boolean assertNotUI() {
        if (!isMainThread()) {
            return true;
        }
        if (App.DEBUG) {
            uncaught(new IllegalStateException("Not main thread required"));
        } else {
            log.get().wtf(new IllegalStateException("Not main thread required"));
        }
        return false;
    }

    @Override
    public void uncaught(Throwable throwable) {
        if (!App.DEBUG) {
            return;
        }
        java.lang.Thread.getDefaultUncaughtExceptionHandler()
                .uncaughtException(java.lang.Thread.currentThread(), throwable);
    }

    private boolean isMainThread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Looper.getMainLooper().isCurrentThread() : java.lang.Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private boolean isLooperThread() {
        return java.lang.Thread.currentThread() == Foreground.thread || java.lang.Thread.currentThread() == Background.thread || isMainThread();
    }

    private ThreadFacade getThreadFacade(@Thread.TYPE int type) {
        switch (type) {
            case BACKGROUND: return Background;
            case FOREGROUND: default: return Foreground;
        }
    }

    private void onCaughtUncaughtException(String thread, Throwable throwable, ThrowingConsumer<Throwable, Throwable> errorHandler) {
        if (errorHandler == null) {
            log.get().exception("Run on " + thread + " thread failed", throwable);
            showToast();
            return;
        }
        log.get().e(TAG, "Run on " + thread + " thread failed | ", throwable.getClass().getName(), ": ", throwable.getMessage());
        // showToast();
        try {
            errorHandler.accept(throwable);
        } catch (Throwable t) {
            log.get().exception("Run on " + thread + " thread failed | run on error consumer failed", t);
            showToast();
        }
    }

    private void showToast() {
        try {
            Toast.makeText(context.get(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
        } catch (Throwable throwable) {
            log.get().exception("ThreadImpl#showToast() failed", throwable);
        }
    }

    private void log(String log) {
        if (DEBUG) {
            android.util.Log.v(TAG, log);
        }
    }

    private static class ThreadFacade {
        public HandlerThread thread = null;
        public final @Thread.TYPE int type;
        public final String name;
        public final int priority;
        public ThreadFacade(@Thread.TYPE int type, String name, int priority) {
            this.type = type;
            this.name = name;
            this.priority = priority;
        }
    }
    private static final ThreadFacade Foreground = new ThreadFacade(FOREGROUND, "CDOExecutorForeground", Process.THREAD_PRIORITY_FOREGROUND);
    private static final ThreadFacade Background = new ThreadFacade(BACKGROUND, "CDOExecutorBackground", Process.THREAD_PRIORITY_BACKGROUND);
}
