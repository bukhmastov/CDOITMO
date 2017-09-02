package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import java.util.Objects;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private final Context context;
    private final int jobID = 0;

    public ProtocolTracker(Context context) {
        this.context = context;
    }

    public ProtocolTracker check() {
        Log.v(TAG, "check");
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"), "1");
        if (enabled && !running) {
            start();
        } else if (!enabled && running) {
            stop();
        } else if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    if (((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(jobID) == null) throw new Exception("job is null");
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                    restart();
                }
            }
        }
        return this;
    }
    public ProtocolTracker restart() {
        Log.v(TAG, "restart");
        stop();
        start();
        return this;
    }
    private ProtocolTracker start() {
        Log.v(TAG, "start");
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"), "1");
        if (enabled && !running) {
            Log.v(TAG, "Starting");
            try {
                int frequency = Integer.parseInt(Storage.pref.get(context, "pref_notify_frequency", "30"));
                boolean network_unmetered = Storage.pref.get(context, "pref_notify_network_unmetered", false);
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, TrackingProtocolJobService.class));
                builder.setPeriodic(frequency * 60000);
                builder.setPersisted(true);
                builder.setRequiredNetworkType(network_unmetered ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY);
                ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(builder.build());
                Storage.file.perm.put(context, "protocol_tracker#job_service_running", "1");
                Storage.file.perm.put(context, "protocol_tracker#protocol", "");
                Log.i(TAG, "Started | user = " + Storage.file.general.get(context, "users#current_login") + " | frequency = " + frequency);
            } catch (Exception e){
                Log.e(TAG, "Failed to schedule job");
                Static.error(e);
                stop();
            }
        }
        return this;
    }
    public ProtocolTracker stop() {
        Log.v(TAG, "stop");
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "0"), "1");
        if (running) {
            Log.v(TAG, "Stopping");
            ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(jobID);
            Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
            Storage.file.perm.put(context, "protocol_tracker#protocol", "");
            Log.i(TAG, "Stopped");
        }
        return this;
    }
    public ProtocolTracker reset() {
        Log.v(TAG, "reset");
        ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancelAll();
        Storage.file.perm.put(context, "protocol_tracker#job_service_running", "0");
        Storage.file.perm.put(context, "protocol_tracker#protocol", "");
        check();
        return this;
    }
}
