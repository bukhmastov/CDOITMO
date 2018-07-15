package com.bukhmastov.cdoitmo.object;

import android.app.job.JobParameters;
import android.app.job.JobService;

import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.util.Log;

public class ProtocolTrackerJobService extends JobService implements Callable {

    private static final String TAG = "ProtocolTrackerJS";
    private JobParameters params = null;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private ProtocolTrackerService protocolTrackerService = ProtocolTrackerService.instance();

    @Override
    public boolean onStartJob(JobParameters params) {
        log.i(TAG, "onStartJob");
        this.params = params;
        protocolTrackerService.request(getBaseContext(), this);
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
