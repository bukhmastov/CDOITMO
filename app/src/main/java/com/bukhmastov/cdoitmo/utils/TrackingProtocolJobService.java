package com.bukhmastov.cdoitmo.utils;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class TrackingProtocolJobService extends JobService {

    private static final String TAG = "ProtocolTrackerJS";
    private static int c = 0;
    private Client.Request requestHandle = null;
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
        if (requestHandle != null) requestHandle.cancel();
        return true;
    }

    private void request() {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "request");
                try {
                    attempt++;
                    if (attempt > maxAttempts) throw new Exception("Number of attempts exceeded the limit");
                    Log.i(TAG, "Request attempt #" + attempt);
                    DeIfmoRestClient.get(getBaseContext(), "eregisterlog?days=2", null, new RestResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, Client.Headers headers, JSONObject responseObj, final JSONArray responseArr) {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
                                @Override
                                public void run() {
                                    if (statusCode == 200 && responseArr != null) {
                                        new ProtocolConverter(getBaseContext(), responseArr, 0, new ProtocolConverter.response() {
                                            @Override
                                            public void finish(JSONObject json) {
                                                try {
                                                    analyse(json.getJSONArray("protocol"));
                                                } catch (JSONException e) {
                                                    Log.e(TAG, e.getMessage());
                                                    done();
                                                }
                                            }
                                        }).run();
                                    } else {
                                        w8andRequest();
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            w8andRequest();
                        }
                        @Override
                        public void onProgress(int state) {}
                        @Override
                        public void onNewRequest(Client.Request request) {
                            requestHandle = request;
                        }
                    });
                } catch (Exception e){
                    Log.e(TAG, e.getMessage());
                    done();
                }
            }
        });
    }
    private void w8andRequest() {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "w8andRequest");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    // just ignore
                }
                request();
            }
        });
    }
    private void analyse(final JSONArray protocol) {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "analyse");
                try {
                    if (protocol == null) throw new NullPointerException("json can't be null");
                    JSONArray history = new JSONArray();
                    boolean first_init = false;
                    try {
                        String historyStr = Storage.file.perm.get(getBaseContext(), "protocol_tracker#protocol");
                        if (Objects.equals(historyStr, "")) {
                            first_init = true;
                        } else {
                            history = new JSONArray(historyStr);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    Storage.file.perm.put(getBaseContext(), "protocol_tracker#protocol", protocol.toString());
                    if (first_init) {
                        done();
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
                            StringBuilder text = new StringBuilder();
                            text.append(changeOBJ.getString("value")).append("/").append(var.getString("max")).append(" — ").append(var.getString("name"));
                            if (changeOBJ.getDouble("cdoitmo_delta_double") != 0) {
                                text.append(" (").append(changeOBJ.getString("cdoitmo_delta")).append(")");
                            }
                            addNotification(changeOBJ.getString("subject"), text.toString(), timestamp, isSummary);
                        }
                    }
                    done();
                } catch (Exception e){
                    done();
                }
            }
        });
    }
    private void addNotification(final String title, final String text, final long timestamp, final boolean isSummary) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "addNotification | title=" + title + " | text=" + text + " | timestamp=" + timestamp + " | isSummary=" + (isSummary ? "true" : "false"));
                if (c > Integer.MAX_VALUE - 10) c = 0;
                // prepare intent
                Intent intent = new Intent(getBaseContext(), SplashActivity.class);
                intent.addFlags(Static.intentFlagRestart);
                intent.putExtra("action", "protocol_changes");
                PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intent, 0);
                // prepare and send notification
                Notifications notifications = new Notifications(getBaseContext());
                notifications.notify(c++, notifications.getProtocol(getBaseContext(), title, text, timestamp, isSummary, pIntent));
            }
        });
    }
    private void done() {
        Static.T.runThread(Static.T.TYPE.BACKGROUND, new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Executed");
                if (requestHandle != null) requestHandle.cancel();
                jobFinished(params, false);
            }
        });
    }
}
