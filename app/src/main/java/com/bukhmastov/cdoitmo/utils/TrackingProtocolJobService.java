package com.bukhmastov.cdoitmo.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.DeIfmoRestClientResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackingProtocolJobService extends JobService {

    private static final String TAG = "TrackingProtocol";
    private static int c = 0;
    private RequestHandle jobRequestHandle = null;
    private int attempt = 0;
    private static final int maxAttempts = 3;
    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Executing");
        this.params = params;
        this.attempt = 0;
        request();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "Stopped");
        if(jobRequestHandle != null) jobRequestHandle.cancel(true);
        return true;
    }

    private void request(){
        try {
            attempt++;
            if (attempt > maxAttempts) throw new Exception("Number of attempts exceeded the limit");
            Log.i(TAG, "Request attempt #" + attempt);
            DeIfmoRestClient.get(this, "eregisterlog?days=2", null, new DeIfmoRestClientResponseHandler() {
                @Override
                public void onSuccess(int statusCode, JSONObject responseObj, JSONArray responseArr) {
                    if (statusCode == 200 && responseArr != null) {
                        analyse(responseArr);
                    } else {
                        w8andRequest();
                    }
                }
                @Override
                public void onProgress(int state) {}
                @Override
                public void onFailure(int state) {
                    w8andRequest();
                }
                @Override
                public void onNewHandle(RequestHandle requestHandle) {
                    jobRequestHandle = requestHandle;
                }
            });
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
            finish();
        }
    }
    private void w8andRequest(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        request();
    }
    private void analyse(JSONArray protocol){
        try {
            if (protocol == null) throw new NullPointerException("json can't be null");
            JSONArray history = new JSONArray();
            boolean first_init = false;
            try {
                String historyStr = Storage.file.perm.get(this, "protocol_tracker#protocol");
                if (Objects.equals(historyStr, "")) {
                    first_init = true;
                } else {
                    history = new JSONArray(historyStr);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
            Storage.file.perm.put(this, "protocol_tracker#protocol", protocol.toString());
            if (first_init) {
                finish();
                return;
            }
            ArrayList<JSONObject> changes = new ArrayList<>();
            for (int i = 0; i < protocol.length(); i++) {
                JSONObject protocolOBJ = protocol.getJSONObject(i);
                boolean found = false;
                for (int j = 0; j < history.length(); j++) {
                    JSONObject historyOBJ = history.getJSONObject(j);
                    if (Objects.equals(historyOBJ.toString(), protocolOBJ.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) changes.add(protocolOBJ);
            }
            if (changes.size() > 0) {
                long timestamp = System.currentTimeMillis();
                boolean isSummary = false;
                if (changes.size() > 1) {
                    String text = changes.size() + " ";
                    switch (changes.size() % 100) {
                        case 10: case 11: case 12: case 13: case 14: text += getString(R.string.action_3); break;
                        default:
                            switch (changes.size() % 10) {
                                case 1: text += getString(R.string.action_1); break;
                                case 2: case 3: case 4: text += getString(R.string.action_2); break;
                                default: text += getString(R.string.action_3); break;
                            }
                            break;
                    }
                    addNotification(getString(R.string.protocol_changes), text, timestamp, true);
                } else {
                    isSummary = true;
                }
                for (int i = changes.size() - 1; i >= 0; i--) {
                    JSONObject changeOBJ = changes.get(i);
                    JSONObject var = changeOBJ.getJSONObject("var");
                    addNotification(changeOBJ.getString("subject"), var.getString("name") + ": " + markConverter(changeOBJ.getString("value")) + "/" + markConverter(var.getString("max")), timestamp, isSummary);
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
        intent.addFlags(Static.intentFlagRestart);
        intent.putExtra("action", "protocol_changes");
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text));
        b.setSmallIcon(R.drawable.cdo_small).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.cdo_small_background));
        b.setGroup("protocol_" + timestamp).setGroupSummary(isSummary);
        b.setCategory(Notification.CATEGORY_EVENT);
        b.setContentIntent(pIntent);
        b.setAutoCancel(true);
        if(isSummary) {
            String ringtonePath = Storage.pref.get(this, "pref_notify_sound");
            if (!Objects.equals(ringtonePath, "")) {
                b.setSound(Uri.parse(ringtonePath));
            }
            if (Storage.pref.get(this, "pref_notify_vibrate", false)) {
                b.setDefaults(Notification.DEFAULT_VIBRATE);
            }
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(c++, b.build());
    }
    private void finish(){
        Log.i(TAG, "Executed");
        if (jobRequestHandle != null) jobRequestHandle.cancel(true);
        jobFinished(this.params, false);
    }
    private String markConverter(String value){
        value = value.replace(",", ".").trim();
        Matcher m = Pattern.compile("^\\.(\\d+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        return value;
    }

}