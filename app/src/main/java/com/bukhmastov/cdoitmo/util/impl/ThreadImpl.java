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
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.Lazy;

public class ThreadImpl implements Thread {

    private static final String TAG = "CDOThreadComposer";
    private static final Map<String, HandlerThreadWrapper> handlerThreadMap = new HashMap<>();
    private static int standaloneThreadNumber = 0;
    private static synchronized int nextStandaloneThreadNum() {
        if (standaloneThreadNumber > Integer.MAX_VALUE - 10) {
            standaloneThreadNumber = 0;
        }
        return standaloneThreadNumber++;
    }

    @Inject
    Lazy<Log> log;
    @Inject
    Lazy<Context> context;
    @Inject
    Lazy<NotificationMessage> notificationMessage;

    public ThreadImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void initialize(@ThreadToken String token) {
        synchronized (handlerThreadMap) {
            HandlerThreadWrapper wrapper = handlerThreadMap.get(token);
            String threadName = makeBackgroundThreadName(token);
            if (wrapper == null || wrapper.isDead() || !wrapper.getThread().isAlive()) {
                HandlerThread handlerThread = new HandlerThread(threadName, Process.THREAD_PRIORITY_FOREGROUND);
                wrapper = new HandlerThreadWrapper(handlerThread);
                wrapper.getThread().start();
                handlerThreadMap.put(token, wrapper);
                log.get().v(TAG, threadName, " | New handler tread initialized");
            }
        }
    }

    @Override
    public void run(@ThreadToken String token, @NonNull ThrowingRunnable runnable) {
        run(token, runnable, null);
    }

    @Override
    public void run(@ThreadToken String token, @NonNull ThrowingRunnable runnable,
                    @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        synchronized (handlerThreadMap) {
            HandlerThreadWrapper wrapper = handlerThreadMap.get(token);
            String threadName = makeBackgroundThreadName(token);
            if (wrapper == null || wrapper.isDead()) {
                log.get().i(TAG, threadName, " | Prevented task execution at background thread, ",
                        "thread not started or not alive");
                return;
            }
            try {
                Handler handler = new Handler(wrapper.getThread().getLooper());
                Runnable task = () -> {
                    try {
                        runnable.run();
                    } catch (Throwable throwable) {
                        log.get().i(TAG, threadName, " | Throwable caught while running the task | ", throwable);
                        onCaughtUncaughtException(threadName, throwable, errorHandler);
                    }
                };
                if (!handler.post(task)) {
                    log.get().w(TAG, threadName, " | Failed to post task to looper's handler. Probably, lopper is exiting right now.");
                    standalone(() -> {
                        try {
                            java.lang.Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.get().i(TAG, threadName, " | InterruptedException caught | ", e);
                        }
                        log.get().v(TAG, threadName, " | Trying to re run the task");
                        run(token, runnable, errorHandler);
                    }, throwable -> {
                        log.get().i(TAG, threadName, " | Throwable caught while trying to re run the task | ", throwable);
                        onCaughtUncaughtException(threadName, throwable, errorHandler);
                    });
                }
            } catch (Throwable throwable) {
                log.get().i(TAG, threadName, " | Throwable caught while posting the task | ", throwable);
                onCaughtUncaughtException(threadName, throwable, errorHandler);
            }
        }
    }

    @Override
    public void runOnUI(String token, @NonNull ThrowingRunnable runnable) {
        runOnUI(token, runnable, null);
    }

    @Override
    public void runOnUI(String token, @NonNull ThrowingRunnable runnable,
                        @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        synchronized (handlerThreadMap) {
            HandlerThreadWrapper wrapper = handlerThreadMap.get(token);
            if (wrapper == null || wrapper.isDead()) {
                String threadName = makeBackgroundThreadName(token);
                log.get().i(TAG, "UI for ", threadName, " | Prevented task execution at UI thread, ",
                        "because corresponding background thread not started or not alive");
                return;
            }
        }
        runOnUI(runnable, errorHandler);
    }

    @Override
    public void interrupt(@ThreadToken String token) {
        synchronized (handlerThreadMap) {
            HandlerThreadWrapper wrapper = handlerThreadMap.get(token);
            if (wrapper == null || wrapper.isDead()) {
                return;
            }
            String threadName = makeBackgroundThreadName(token);
            try {
                wrapper.getThread().quit();
                wrapper.getThread().interrupt();
            } catch (Throwable throwable) {
                log.get().i(TAG, threadName, " | Throwable caught while interrupting handler thread | ", throwable);
            }
            wrapper.die();
            handlerThreadMap.remove(token);
            log.get().v(TAG, threadName, " | Handler tread interrupted");
        }
    }

    @Override
    public void runOnUI(@NonNull ThrowingRunnable runnable) {
        runOnUI(runnable, null);
    }

    @Override
    public void runOnUI(@NonNull ThrowingRunnable runnable,
                        @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        if (isMainThread()) {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                log.get().i(TAG, "UI thread | Throwable caught while running the task | ", throwable);
                onCaughtUncaughtException("main", throwable, errorHandler);
            }
            return;
        }
        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    runnable.run();
                } catch (Throwable throwable) {
                    log.get().i(TAG, "UI thread | Throwable caught while running the task | ", throwable);
                    onCaughtUncaughtException("main", throwable, errorHandler);
                }
            });
        } catch (Throwable throwable) {
            log.get().i(TAG, "UI thread | Throwable caught while posting the task | ", throwable);
            onCaughtUncaughtException("main", throwable, errorHandler);
        }
    }

    @Override
    public void standalone(@NonNull ThrowingRunnable runnable) {
        standalone(runnable, null);
    }

    @Override
    public void standalone(@NonNull ThrowingRunnable runnable,
                           @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler) {
        String threadName = String.format(Locale.getDefault(), THREAD_NAME_STANDALONE, nextStandaloneThreadNum());
        try {
            if (App.DEBUG && isStandaloneThread()) {
                log.get().w(TAG, threadName, " | Standalone thread going to create new standalone thread, what a shame!!!");
            }
            java.lang.Thread thread = new java.lang.Thread(() -> {
                try {
                    log.get().v(TAG, threadName, " | New standalone thread has started");
                    runnable.run();
                    log.get().v(TAG, threadName, " | Standalone thread has ended");
                } catch (Throwable throwable) {
                    onCaughtUncaughtException(threadName, throwable, errorHandler);
                }
            });
            thread.setName(threadName);
            thread.setPriority(java.lang.Thread.MIN_PRIORITY);
            thread.start();
        } catch (Throwable throwable) {
            log.get().i(TAG, threadName, " | Throwable caught while starting the thread | ", throwable);
            onCaughtUncaughtException(threadName, throwable, errorHandler);
        }
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                Looper.getMainLooper().isCurrentThread() :
                java.lang.Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    private boolean isStandaloneThread() {
        return java.lang.Thread.currentThread().getName().startsWith(THREAD_NAME_STANDALONE_PREFIX);
    }

    private String makeBackgroundThreadName(@ThreadToken String token) {
        return String.format(THREAD_NAME_BACKGROUND, token);
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

    private class HandlerThreadWrapper {
        private HandlerThread thread;
        private boolean isAlive;
        public HandlerThreadWrapper(HandlerThread thread) {
            this.thread = thread;
            this.isAlive = true;
        }
        public HandlerThread getThread() {
            return thread;
        }
        public boolean isDead() {
            return !isAlive;
        }
        public void die() {
            isAlive = false;
            thread = null;
        }
    }
}
