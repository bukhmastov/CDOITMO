package com.bukhmastov.cdoitmo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrackingProtocolJobService extends JobService {

    private static final String TAG = "TrackingProtocol";
    private static int c = 0;
    private SharedPreferences sharedPreferences;
    private RequestHandle jobRequestHandle = null;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Executing");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            DeIfmoRestClient.init(getApplicationContext());
            DeIfmoRestClient.authorize(new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, String response) {
                    Calendar now = Calendar.getInstance();
                    int year = now.get(Calendar.YEAR);
                    int month = now.get(Calendar.MONTH);
                    RequestParams rParams = new RequestParams();
                    rParams.put("Rule", "eRegisterGetProtokolVariable");
                    rParams.put("ST_GRP", sharedPreferences.getString("group", ""));
                    rParams.put("PERSONID", sharedPreferences.getString("login", ""));
                    rParams.put("SYU_ID", "0");
                    rParams.put("UNIVER", "1");
                    rParams.put("APPRENTICESHIP", month > Calendar.AUGUST ? year + "/" + (year + 1) : (year - 1) + "/" + year);
                    rParams.put("PERIOD", "7");
                    DeIfmoRestClient.post("servlet/distributedCDE", rParams, new DeIfmoRestClientResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, String response) {
                            if (statusCode == 200) {
                                new ProtocolParse(new ProtocolParse.response() {
                                    @Override
                                    public void finish(JSONObject json) {
                                        analyse(json);
                                    }
                                }).execute(response);
                            } else {
                                finish();
                            }
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onFailure(int state) {
                            finish();
                        }
                        @Override
                        public void onNewHandle(RequestHandle requestHandle) {
                            jobRequestHandle = requestHandle;
                        }
                    });
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onFailure(int state) {
                    finish();
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    jobRequestHandle = requestHandle;
                }
            });
        } catch (Exception e){
            finish();
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Stopped");
        if(jobRequestHandle != null) jobRequestHandle.cancel(true);
        return true;
    }

    private void analyse(JSONObject json){
        try {
            if(json == null) throw new NullPointerException("json can't be null");
            JSONArray history = new JSONArray();
            try {
                String historyStr = sharedPreferences.getString("TrackingProtocolJobServiceHISTORY", "");
                if(!Objects.equals(historyStr, "")) history = new JSONArray(historyStr);
            } catch(Exception e){
                e.printStackTrace();
            }
            JSONArray protocol = json.getJSONArray("changes");
            sharedPreferences.edit().putString("TrackingProtocolJobServiceHISTORY", protocol.toString()).apply();
            int id = 0;
            for(int i = 0; i < history.length(); i++){
                JSONObject historyOBJ = history.getJSONObject(i);
                for(int j = 0; j < protocol.length(); j++){
                    JSONObject protocolOBJ = protocol.getJSONObject(i);
                    if(Objects.equals(historyOBJ.getString("subject"), protocolOBJ.getString("subject")) &&
                       Objects.equals(historyOBJ.getString("field"), protocolOBJ.getString("field")) &&
                       Objects.equals(historyOBJ.getString("value"), protocolOBJ.getString("value")) &&
                       Objects.equals(historyOBJ.getString("date"), protocolOBJ.getString("date"))){
                        id = j;
                        break;
                    }
                }
                if(id != 0) break;
            }
            if(id > 0){
                long timestamp = System.currentTimeMillis();
                if(id > 1){
                    String text = id + " ";
                    switch (id % 100){
                        case 10: case 11: case 12: case 13: case 14: text += getString(R.string.action_3); break;
                        default:
                            switch (id % 10){
                                case 1: text += getString(R.string.action_1); break;
                                case 2: case 3: case 4: text += getString(R.string.action_2); break;
                                default: text += getString(R.string.action_3); break;
                            }
                            break;
                    }
                    addNotification(getString(R.string.protocol_changes), text, timestamp, true);
                }
                for(int i = id - 1; i >= 0; i--){
                    JSONObject protocolOBJ = protocol.getJSONObject(i);
                    addNotification(protocolOBJ.getString("subject"), protocolOBJ.getString("field") + ": " + double2string(protocolOBJ.getDouble("value")) + "/" + double2string(protocolOBJ.getDouble("max")), timestamp, false);
                }
            }
            finish();
        } catch (Exception e){
            finish();
        }
    }
    private void addNotification(String title, String text, long timestamp, boolean isSummary){
        if(c > Integer.MAX_VALUE - 10) c = 0;
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text));
        b.setSmallIcon(R.drawable.cdo_small).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.cdo_small_background));
        b.setGroup("protocol_" + timestamp).setGroupSummary(isSummary);
        b.setCategory(Notification.CATEGORY_EVENT);
        b.setContentIntent(pIntent);
        b.setAutoCancel(true);
        String ringtonePath = sharedPreferences.getString("pref_notify_sound", "");
        if(!Objects.equals(ringtonePath, "")){
            b.setSound(Uri.parse(ringtonePath));
        }
        if(sharedPreferences.getBoolean("pref_notify_vibrate", false)){
            b.setDefaults(Notification.DEFAULT_VIBRATE);
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(c++, b.build());
    }
    private void finish(){
        Log.i(TAG, "Executed");
        if(jobRequestHandle != null) jobRequestHandle.cancel(true);
        this.stopSelf();
    }
    private String double2string(Double value){
        String valueStr = String.valueOf(value);
        if(value != -1.0){
            if(value == Double.parseDouble(value.intValue() + ".0")){
                valueStr = value.intValue() + "";
            }
        } else {
            valueStr = "-";
        }
        return valueStr;
    }
}

class ProtocolTracker {

    private static final String TAG = "ProtocolTracker";
    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean enabled = true;
    private boolean running = false;
    private int frequency = 30;
    private int jobID = -1;

    ProtocolTracker(Context context){
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        update();
    }

    ProtocolTracker update(){
        enabled = sharedPreferences.getBoolean("pref_use_notifications", true);
        frequency = Integer.parseInt(sharedPreferences.getString("pref_notify_frequency", "30"));
        jobID = sharedPreferences.getInt("TrackingProtocolJobServiceID", -1);
        running = jobID != -1;
        return this;
    }
    ProtocolTracker check(){
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
                    restart();
                }
            }
        }
        return this;
    }
    ProtocolTracker restart(){
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
                        .putString("TrackingProtocolJobServiceHISTORY", "")
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
    ProtocolTracker stop(){
        if(running){
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(jobID);
            jobID = -1;
            running = false;
            sharedPreferences.edit().putInt("TrackingProtocolJobServiceID", jobID).apply();
            Log.i(TAG, "Stopped");
        }
        return this;
    }
}