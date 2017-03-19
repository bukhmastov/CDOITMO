package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Objects;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private Context context;
    private int jobID = 0;

    public ProtocolTracker(Context context){
        this.context = context;
    }

    public ProtocolTracker check(){
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "false"), "true");
        if (enabled && !running) {
            start();
        }
        else if (!enabled && running) {
            stop();
        }
        else if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    if (((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(jobID) == null) throw new Exception("job is null");
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) restart();
                }
            }
        }
        return this;
    }
    public ProtocolTracker restart(){
        stop();
        start();
        return this;
    }
    private ProtocolTracker start(){
        boolean enabled = Storage.pref.get(context, "pref_use_notifications", true);
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "false"), "true");
        int frequency = Integer.parseInt(Storage.pref.get(context, "pref_notify_frequency", "30"));
        if (enabled && !running) {
            try {
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, TrackingProtocolJobService.class));
                builder.setPeriodic(frequency * 60000);
                builder.setPersisted(true);
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(builder.build());
                Storage.file.perm.put(context, "protocol_tracker#job_service_running", "true");
                Storage.file.perm.put(context, "protocol_tracker#protocol", "");
                Log.i(TAG, "Started | user = " + Storage.file.general.get(context, "users#current_login") + " | frequency = " + frequency);
            } catch (Exception e){
                Log.e(TAG, "Failed to schedule job");
                e.printStackTrace();
                stop();
            }
        }
        return this;
    }
    public ProtocolTracker stop(){
        boolean running = Objects.equals(Storage.file.perm.get(context, "protocol_tracker#job_service_running", "false"), "true");
        if (running) {
            ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(jobID);
            Storage.file.perm.put(context, "protocol_tracker#job_service_running", "false");
            Storage.file.perm.put(context, "protocol_tracker#protocol", "");
            Log.i(TAG, "Stopped");
        }
        return this;
    }
}