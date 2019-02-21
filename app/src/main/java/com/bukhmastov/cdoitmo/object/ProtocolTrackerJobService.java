package com.bukhmastov.cdoitmo.object;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class ProtocolTrackerJobService extends JobService implements Callable {

    private static final String TAG = "ProtocolTrackerJS";
    private JobParameters params = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    ProtocolTrackerService protocolTrackerService;

    @Override
    public boolean onStartJob(JobParameters params) {
        AppComponentProvider.getComponent().inject(this);
        this.params = params;
        thread.standalone(() -> {
            log.i(TAG, "onStartJob");
            protocolTrackerService.request(getBaseContext(), this);
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        log.i(TAG, "onStopJob");
        protocolTrackerService.shutdown();
        return true;
    }

    @Override
    public void call() {
        log.i(TAG, "jobFinished");
        if (params != null) {
            jobFinished(params, false);
        } else {
            log.exception(TAG, new NullPointerException("JobParameters is null at the finish"));
        }
    }
}
