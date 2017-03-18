package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private Context context;
    private boolean enabled = true;
    private boolean running = false;
    private int frequency = 30;
    private int jobID = -1;

    public ProtocolTracker(Context context){
        this.context = context;
        update();
    }

    public ProtocolTracker update(){
        enabled = Storage.pref.get(context, "pref_use_notifications", true);
        frequency = Integer.parseInt(Storage.pref.get(context, "pref_notify_frequency", "30"));
        jobID = Integer.parseInt(Storage.file.perm.get(context, "protocol_tracker#job_service_id", "-1"));
        running = jobID != -1;
        return this;
    }
    public ProtocolTracker check(){
        if(enabled && !running){
            start();
        }
        else if(!enabled && running){
            stop();
        }
        else if(enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    if (jobScheduler.getPendingJob(jobID) == null) throw new Exception("job is null");
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) restart();
                }
            }
        }
        return this;
    }
    public ProtocolTracker restart(){
        stop();
        if(enabled) start();
        return this;
    }
    private ProtocolTracker start(){
        if(!running){
            try {
                JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, TrackingProtocolJobService.class));
                builder.setPeriodic(frequency * 60000);
                builder.setPersisted(true);
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
                JobInfo info = builder.build();
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                jobScheduler.schedule(info);
                jobID = info.getId();
                running = true;
                Storage.file.perm.put(context, "protocol_tracker#job_service_id", String.valueOf(jobID));
                Storage.file.perm.put(context, "protocol_tracker#protocol", "");
                Log.i(TAG, "Started | frequency = " + frequency + " | jobID = " + jobID);
            } catch (Exception e){
                Log.e(TAG, "Failed to schedule job");
                e.printStackTrace();
                stop();
            }
        }
        return this;
    }
    public ProtocolTracker stop(){
        if(running){
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(jobID);
            jobID = -1;
            running = false;
            Storage.file.perm.put(context, "protocol_tracker#job_service_id", String.valueOf(jobID));
            Storage.file.perm.delete(context, "protocol_tracker#protocol");
            Log.i(TAG, "Stopped");
        }
        return this;
    }
}