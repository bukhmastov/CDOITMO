package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private final @NonNull Context context;
    private final @NonNull JobScheduler jobScheduler;
    private final int jobID = 0;

    public ProtocolTracker(@NonNull Context context) {
        final JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) {
            throw new RuntimeException("JobScheduler is null. Unable to use ProtocolTracker");
        }
        this.context = context;
        this.jobScheduler = js;
    }

    public ProtocolTracker check() {
        return check(null);
    }
    public ProtocolTracker check(@Nullable final Static.SimpleCallback callback) {
        Log.v(TAG, "check");
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            start(callback);
        } else if (!enabled && running) {
            stop(callback);
        } else if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                if (jobScheduler.getPendingJob(jobID) == null) throw new Exception("job is null");
                if (callback != null) {
                    callback.onCall();
                }
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
                restart(callback);
            }
        } else {
            if (callback != null) {
                callback.onCall();
            }
        }
        return this;
    }

    public ProtocolTracker restart() {
        return restart(null);
    }
    public ProtocolTracker restart(@Nullable final Static.SimpleCallback callback) {
        Log.v(TAG, "restart");
        stop(new Static.SimpleCallback() {
            @Override
            public void onCall() {
                start(callback);
            }
        });
        return this;
    }

    private ProtocolTracker start() {
        return start(null);
    }
    private ProtocolTracker start(@Nullable final Static.SimpleCallback callback) {
        Log.v(TAG, "start");
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (enabled && !running) {
            Log.v(TAG, "Starting");
            try {
                int frequency = Integer.parseInt(Storage.pref.get(context, "pref_notify_frequency", "30"));
                boolean network_unmetered = Storage.pref.get(context, "pref_notify_network_unmetered", false);
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, TrackingProtocolJobService.class));
                builder.setPeriodic(frequency * 60000);
                builder.setPersisted(true);
                builder.setRequiredNetworkType(network_unmetered ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY);
                jobScheduler.schedule(builder.build());
                Storage.file.perm.put(context, "protocol_tracker#job_service_running", "1");
                Storage.file.perm.put(context, "protocol_tracker#protocol", "");
                Log.i(TAG, "Started | user = " + Storage.file.general.get(context, "users#current_login") + " | frequency = " + frequency);
                if (callback != null) {
                    callback.onCall();
                }
            } catch (Exception e){
                Log.e(TAG, "Failed to schedule job");
                Static.error(e);
                stop(callback);
            }
        } else {
            if (callback != null) {
                callback.onCall();
            }
        }
        return this;
    }

    public ProtocolTracker stop() {
        return stop(null);
    }
    public ProtocolTracker stop(@Nullable final Static.SimpleCallback callback) {
        Log.v(TAG, "stop");
        boolean running = "1".equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"));
        if (running) {
            Log.v(TAG, "Stopping");
            jobScheduler.cancel(jobID);
            Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
            Storage.file.perm.put(context, "protocol_tracker#protocol", "");
            Log.i(TAG, "Stopped");
        }
        if (callback != null) {
            callback.onCall();
        }
        return this;
    }

    public ProtocolTracker reset() {
        return reset(null);
    }
    public ProtocolTracker reset(@Nullable final Static.SimpleCallback callback) {
        Log.v(TAG, "reset");
        jobScheduler.cancelAll();
        Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
        Storage.file.perm.put(context, "protocol_tracker#protocol", "");
        check(callback);
        return this;
    }
}
