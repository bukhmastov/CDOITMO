package com.bukhmastov.cdoitmo.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean enabled = true;
    private boolean running = false;
    private int frequency = 30;
    private int jobID = -1;

    public ProtocolTracker(Context context){
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        update();
    }

    public ProtocolTracker update(){
        enabled = sharedPreferences.getBoolean("pref_use_notifications", true);
        frequency = Integer.parseInt(sharedPreferences.getString("pref_notify_frequency", "30"));
        jobID = sharedPreferences.getInt("TrackingProtocolJobServiceID", -1);
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
                sharedPreferences.edit()
                        .putInt("TrackingProtocolJobServiceID", jobID)
                        .putString("ProtocolTrackerHISTORY", "")
                        .apply();
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
            sharedPreferences.edit().putInt("TrackingProtocolJobServiceID", jobID).remove("ProtocolTrackerHISTORY").apply();
            Log.i(TAG, "Stopped");
        }
        return this;
    }
}