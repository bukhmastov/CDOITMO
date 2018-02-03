package com.bukhmastov.cdoitmo.utils;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.converters.ProtocolConverter;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.interfaces.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.models.Client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProtocolTrackerJobService extends JobService {

    private static final String TAG = "ProtocolTrackerJS";
    private static int notificationId = 0;
    private JobParameters params = null;
    private Client.Request requestHandle = null;
    private int attempt = 0;
    private static final int maxAttempts = 3;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "Started");
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
        Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
            try {
                attempt++;
                if (attempt > maxAttempts) throw new Exception("Number of attempts exceeded the limit");
                Log.v(TAG, "request | attempt #" + attempt);
                DeIfmoRestClient.get(getBaseContext(), "eregisterlog?days=2", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, Client.Headers headers, JSONObject responseObj, final JSONArray responseArr) {
                        try {
                            Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                                try {
                                    if (statusCode == 200 && responseArr != null) {
                                        new ProtocolConverter(getBaseContext(), responseArr, 0, json -> {
                                            try {
                                                handle(json.getJSONArray("protocol"));
                                            } catch (Exception e) {
                                                Log.w(TAG, "request | catch(onSuccess, Thread, ProtocolConverter) | " + e.getMessage());
                                                finish();
                                            }
                                        }).run();
                                    } else {
                                        w8andRequest();
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "request | catch(onSuccess, Thread) | " + e.getMessage());
                                    finish();
                                }
                            });
                        } catch (Exception e) {
                            Log.w(TAG, "request | catch(onSuccess) | " + e.getMessage());
                            finish();
                        }
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
                Log.w(TAG, "request | catch | " + e.getMessage());
                finish();
            }
        });
    }
    private void w8andRequest() {
        try {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                try {
                    Log.v(TAG, "w8andRequest");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                        // just ignore
                    }
                    request();
                } catch (Exception e) {
                    Log.w(TAG, "w8andRequest | catch(Thread) | " + e.getMessage());
                    finish();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "w8andRequest | catch | " + e.getMessage());
            finish();
        }
    }
    private void handle(final JSONArray protocol) {
        try {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                try {
                    Log.v(TAG, "handle");
                    if (protocol == null) throw new NullPointerException("json can't be null");
                    // step 1
                    // fetching previous protocol value
                    // saving current protocol value for future fetching
                    // preventing displaying notifications, if there is no previous protocol value
                    JSONArray previousProtocol = new JSONArray();
                    boolean firstInit = false;
                    try {
                        final String previousProtocolValue = Storage.file.perm.get(this, "protocol_tracker#protocol");
                        if (previousProtocolValue.isEmpty()) {
                            firstInit = true;
                        } else {
                            previousProtocol = new JSONArray(previousProtocolValue);
                        }
                    } catch (Exception ignore) {/* ignore */}
                    Storage.file.perm.put(this, "protocol_tracker#protocol", protocol.toString());
                    if (firstInit) {
                        finish();
                        return;
                    }
                    // step 2
                    // creating list of changes based on current and previous protocol values
                    Map<String, ArrayList<JSONObject>> changes = new HashMap<>();
                    for (int i = 0; i < protocol.length(); i++) {
                        final JSONObject currentChange = protocol.getJSONObject(i);
                        boolean found = false;
                        for (int j = 0; j < previousProtocol.length(); j++) {
                            final JSONObject previousChange = previousProtocol.getJSONObject(j);
                            if (currentChange.toString().equals(previousChange.toString())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            final String subject = currentChange.getString("subject");
                            ArrayList<JSONObject> changes4subject = changes.containsKey(subject) ? changes.get(subject) : new ArrayList<>();
                            changes4subject.add(currentChange);
                            changes.put(subject, changes4subject);
                        }
                    }
                    // step 3
                    // creating notifications for existing new changes
                    if (changes.size() > 0) {
                        final long timestamp = System.currentTimeMillis();
                        final String pref_notify_type = Storage.pref.get(this, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
                        if ("0".equals(pref_notify_type)) {
                            // show single notification per subject that contains all changes related to that subject
                            // best suitable: android <= 6.0 and android == 8.0
                            int index = 0;
                            for (Map.Entry<String, ArrayList<JSONObject>> change : changes.entrySet()) {
                                final String subject = change.getKey();
                                final ArrayList<JSONObject> changes4subject = change.getValue();
                                StringBuilder text = new StringBuilder();
                                for (int i = changes4subject.size() - 1; i >= 0; i--) {
                                    // show up to 8 changes
                                    // for more changes placeholder will be shown: "+13 событий"
                                    if (i > changes4subject.size() - 9) {
                                        text.append(change2string(changes4subject.get(i)));
                                        if (i > 0) {
                                            text.append("\n");
                                        }
                                    } else {
                                        text.append("+").append(i + 1).append(" ").append(getActionsLabel(i + 1));
                                        break;
                                    }
                                }
                                addNotification(subject, text.toString(), timestamp, index, true);
                                index++;
                            }
                        } else {
                            // show notifications per each change that grouped together
                            // best suitable: android >= 7.0 except android == 8.0
                            int size = 0;
                            for (Map.Entry<String, ArrayList<JSONObject>> change : changes.entrySet()) {
                                size += change.getValue().size();
                            }
                            if (size > 1) {
                                addNotification(getString(R.string.protocol_changes), String.valueOf(size) + " " + getActionsLabel(size), timestamp, 0, true);
                            }
                            for (Map.Entry<String, ArrayList<JSONObject>> change : changes.entrySet()) {
                                final String subject = change.getKey();
                                final ArrayList<JSONObject> changes4subject = change.getValue();
                                for (int i = changes4subject.size() - 1; i >= 0; i--) {
                                    addNotification(subject, change2string(changes4subject.get(i)), timestamp, 0, size == 1);
                                }
                            }
                        }
                    }
                    // step 4
                    // we are done
                    finish();
                } catch (Exception e) {
                    Log.w(TAG, "handle | catch(Thread) | " + e.getMessage());
                    finish();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "handle | catch | " + e.getMessage());
            finish();
        }
    }
    private void addNotification(final String title, final String text, final long timestamp, final int group, final boolean isSummary) {
        try {
            Static.T.runThread(() -> {
                try {
                    Log.v(TAG, "addNotification | title=" + title + " | text=" + text.replaceAll("\n", "\\n") + " | timestamp=" + timestamp + " | isSummary=" + (isSummary ? "true" : "false"));
                    if (notificationId > Integer.MAX_VALUE - 10) notificationId = 0;
                    // prepare intent
                    Intent intent = new Intent(getBaseContext(), MainActivity.class);
                    intent.addFlags(Static.intentFlagRestart);
                    intent.putExtra("action", "protocol_changes");
                    PendingIntent pIntent = PendingIntent.getActivity(getBaseContext(), (int) System.currentTimeMillis(), intent, 0);
                    // prepare and send notification
                    Notifications notifications = new Notifications(getBaseContext());
                    notifications.notify(notificationId++, notifications.getProtocol(getBaseContext(), title, text, timestamp, group, isSummary, pIntent));
                } catch (Exception e) {
                    Log.w(TAG, "addNotification | catch(Thread) | " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "addNotification | catch | " + e.getMessage());
        }
    }
    private void finish() {
        try {
            Static.T.runThread(Static.T.TYPE.BACKGROUND, () -> {
                try {
                    Log.i(TAG, "Executed");
                    if (requestHandle != null) requestHandle.cancel();
                } catch (Exception e) {
                    Log.w(TAG, "finish | catch(Thread) | " + e.getMessage());
                } finally {
                    if (params != null) {
                        jobFinished(params, false);
                    } else {
                        Log.exception(TAG, new NullPointerException("JobService.params is null at the finish"));
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "finish | catch | " + e.getMessage());
            if (params != null) {
                jobFinished(params, false);
            } else {
                Log.exception(TAG, new NullPointerException("JobService.params is null at the finish"));
            }
        }
    }

    private String change2string(JSONObject change) throws Exception {
        StringBuilder text = new StringBuilder();
        JSONObject var = change.getJSONObject("var");
        text.append(change.getString("value")).append("/").append(var.getString("max")).append(" — ").append(var.getString("name"));
        if (change.getDouble("cdoitmo_delta_double") != 0) {
            text.append(" (").append(change.getString("cdoitmo_delta")).append(")");
        }
        return text.toString();
    }
    private String getActionsLabel(int size) throws Exception {
        StringBuilder text = new StringBuilder();
        switch (size % 100) {
            case 10: case 11: case 12: case 13: case 14: text.append(getString(R.string.action_3)); break;
            default:
                switch (size % 10) {
                    case 1: text.append(getString(R.string.action_1)); break;
                    case 2: case 3: case 4: text.append(getString(R.string.action_2)); break;
                    default: text.append(getString(R.string.action_3)); break;
                }
                break;
        }
        return text.toString();
    }
}
