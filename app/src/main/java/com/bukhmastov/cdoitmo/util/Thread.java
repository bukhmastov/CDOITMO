package com.bukhmastov.cdoitmo.util;

import com.bukhmastov.cdoitmo.function.ThrowingConsumer;
import com.bukhmastov.cdoitmo.function.ThrowingRunnable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

/**
 * App has multiple threads, but performs tasks consistently task by task.
 *
 * All code-managed app threads divided to 3 groups:
 * 1. Main thread (UI)
 *    Should contain only tasks, that going to update user interface.
 *    All tasks performed consistently at one looper thread.
 * 2. Background thread
 *    Should contain tasks, that preparing data for user interface.
 *    All tasks with equal {@link ThreadToken} performed consistently at one looper thread.
 * 3. Standalone thread
 *    Tasks, that do heavy work and not required to be in queue.
 *    Tasks cannot know which state the application will be at the end of the task.
 *    Each task performed at standalone thread.
 *
 * Each background task (not standalone) should be executed at corresponding looper thread, defined
 * wia {@link ThreadToken}. Each UI task, that started from background task, should be executed
 * at main looper thread wia {@link ThreadToken}. If background looper thread with {@link ThreadToken}
 * has been terminated, UI task will not be executed.
 * Tasks, that performed wia equal {@link ThreadToken}, will be in same queue and executed one by one.
 * Tasks, that performed wia different {@link ThreadToken}, will be executed in parallel wia different
 * threads.
 * Tasks, that performed wia standalone thread, will be executed in parallel, using new non-looper thread.
 */
public interface Thread {

    void initialize(@ThreadToken String token);

    void run(@ThreadToken String token, @NonNull ThrowingRunnable runnable);

    void run(@ThreadToken String token, @NonNull ThrowingRunnable runnable,
             @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void runOnUI(@ThreadToken String token, @NonNull ThrowingRunnable runnable);

    void runOnUI(@ThreadToken String token, @NonNull ThrowingRunnable runnable,
                 @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void interrupt(@ThreadToken String token);

    void runOnUI(@NonNull ThrowingRunnable runnable);

    void runOnUI(@NonNull ThrowingRunnable runnable,
                 @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    void standalone(@NonNull ThrowingRunnable runnable);

    void standalone(@NonNull ThrowingRunnable runnable,
                    @Nullable ThrowingConsumer<Throwable, Throwable> errorHandler);

    boolean assertUI();

    boolean assertNotUI();

    /**
     * Will crash the application
     * Only for debugging purposes
     * Will not take affect if debug mode turned off
     * @see com.bukhmastov.cdoitmo.App.DEBUG
     */
    void uncaught(Throwable throwable);

    String THREAD_NAME_STANDALONE = "CDO-standalone-%d";
    String THREAD_NAME_BACKGROUND = "CDO-%s";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ER, ERS, PR, RA, RAL, R101, SL, SLM, SLS, SE, SA, IGR,
            ISSA, ISSP, ISSPD, UH, UP, UF, UU, UN, UE, UB, UPC,
            AM, AL, AS, AHS,
            WDR, WTR, WSL, WSLC
    })
    @interface ThreadToken {}
    String ER = "eregister";
    String ERS = "eregister-subject";
    String PR = "protocol";
    String RA = "rating";
    String RAL = "rating-list";
    String SL = "schedule-lessons";
    String SLM = "schedule-lessons-modify";
    String SLS = "schedule-lessons-share";
    String SE = "schedule-exams";
    String SA = "schedule-attestations";
    String R101 = "room101";
    String IGR = "isu-group";
    String ISSA = "isu-scholarship-assigned";
    String ISSP = "isu-scholarship-paid";
    String ISSPD = "isu-scholarship-paid-details";
    String UH = "university-host";
    String UP = "university-persons";
    String UF = "university-faculties";
    String UU = "university-units";
    String UN = "university-news";
    String UE = "university-events";
    String UB = "university-buildings";
    String UPC = "university-person-card";
    String AM = "activity-main";
    String AL = "activity-login";
    String AS = "activity-search";
    String AHS = "activity-home-screen";
    String WDR = "widget-days-remaining";
    String WTR = "widget-time-remaining";
    String WSL = "widget-sl";
    String WSLC = "widget-sl-config";
}
