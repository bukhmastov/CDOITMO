package com.bukhmastov.cdoitmo.object.impl;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.model.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.model.protocol.PChange;
import com.bukhmastov.cdoitmo.model.protocol.Protocol;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.ProtocolTrackerService;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import dagger.Lazy;

public class ProtocolTrackerServiceImpl implements ProtocolTrackerService {

    private static final String TAG = "ProtocolTrackerService";
    private static int notificationId = 0;
    private Callable callback = null;
    private Client.Request requestHandle = null;
    private int attempt = 0;
    private static final int maxAttempts = 3;
    private String trace = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Lazy<Storage> storage;
    @Inject
    Lazy<StoragePref> storagePref;
    @Inject
    DeIfmoRestClient deIfmoRestClient;
    @Inject
    Lazy<Notifications> notifications;
    @Inject
    Lazy<Time> time;
    @Inject
    FirebasePerformanceProvider firebasePerformanceProvider;

    public ProtocolTrackerServiceImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void request(@NonNull final Context context, Callable callback) {
        this.callback = callback;
        this.attempt = 0;
        request(context);
    }

    @Override
    public void shutdown() {
        try {
            log.v(TAG, "shutdown");
            if (requestHandle != null) requestHandle.cancel();
            firebasePerformanceProvider.stopTrace(trace);
        } catch (Throwable throwable) {
            log.w(TAG, "shutdown | catch | ", throwable.getMessage());
        }
    }

    private void request(@NonNull Context context) {
        try {
            trace = firebasePerformanceProvider.startTrace(FirebasePerformanceProvider.Trace.PROTOCOL_TRACKER);
            thread.run(thread.BACKGROUND, () -> {
                attempt++;
                if (attempt > maxAttempts) {
                    throw new Exception("Number of attempts exceeded the limit");
                }
                log.v(TAG, "request | attempt #" + attempt);
                deIfmoRestClient.get(context, "eregisterlog?days=2", null, new RestResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, Client.Headers headers, JSONObject obj, final JSONArray arr) {
                        try {
                            thread.run(thread.BACKGROUND, () -> {
                                if (statusCode == 200 && arr != null) {
                                    Protocol protocol = new Protocol().fromJson(new JSONObject().put("protocol", arr));
                                    protocol.setTimestamp(time.get().getTimeInMillis());
                                    protocol.setNumberOfWeeks(0);
                                    protocol = new ProtocolConverter(protocol).convert();
                                    try {
                                        handle(context, protocol);
                                    } catch (Exception e) {
                                        log.w(TAG, "request | catch(onSuccess, Thread, ProtocolConverter) | ", e.getMessage());
                                        finish();
                                    }
                                } else {
                                    w8andRequest(context);
                                }
                            }, throwable -> {
                                log.w(TAG, "request | catch(onSuccess, Thread) | ", throwable.getMessage());
                                finish();
                            });
                        } catch (Throwable throwable) {
                            log.w(TAG, "request | catch(onSuccess) | ", throwable.getMessage());
                            finish();
                        }
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        if (state != DeIfmoRestClient.FAILED_INTERRUPTED) {
                            w8andRequest(context);
                        } else {
                            finish();
                        }
                    }
                    @Override
                    public void onProgress(int state) {}
                    @Override
                    public void onNewRequest(Client.Request request) {
                        requestHandle = request;
                    }
                });
            }, throwable -> {
                log.w(TAG, "request | catch(Thread) | ", throwable.getMessage());
                finish();
            });
        } catch (Throwable throwable) {
            log.w(TAG, "request | catch | ", throwable.getMessage());
            finish();
        }
    }

    private void w8andRequest(@NonNull Context context) {
        try {
            firebasePerformanceProvider.stopTrace(trace);
            thread.run(thread.BACKGROUND, () -> {
                log.v(TAG, "w8andRequest");
                try {
                    thread.sleep(1000);
                } catch (InterruptedException ignore) {
                    // just ignore
                }
                request(context);
            }, throwable -> {
                log.w(TAG, "w8andRequest | catch(Thread) | ", throwable.getMessage());
                finish();
            });
        } catch (Exception e) {
            log.w(TAG, "w8andRequest | catch | ", e.getMessage());
            finish();
        }
    }

    private void handle(@NonNull Context context, Protocol protocol) {
        try {
            thread.run(thread.BACKGROUND, () -> {
                log.v(TAG, "handle");
                if (protocol == null) {
                    throw new NullPointerException("protocol cannot be null");
                }
                // step 1
                // fetching previous protocol value
                // saving current protocol value for future fetching
                // preventing displaying notifications, if there is no previous protocol value
                Protocol protocolPrevious = null;
                try {
                    String previousProtocolValue = storage.get().get(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol");
                    if (StringUtils.isNotBlank(previousProtocolValue)) {
                        protocolPrevious = new Protocol().fromJsonString(previousProtocolValue);
                    }
                } catch (Exception ignore) {/* ignore */}
                storage.get().put(context, Storage.PERMANENT, Storage.USER, "protocol_tracker#protocol", protocol.toJsonString());
                if (protocolPrevious == null) {
                    finish();
                    return;
                }
                // step 2
                // creating list of changes based on current and previous protocol values
                List<PChange> pChanges = protocol.getChanges();
                List<PChange> pChangesPrev = CollectionUtils.emptyIfNull(protocolPrevious.getChanges());
                Map<String, ArrayList<PChange>> changes = new HashMap<>();
                if (CollectionUtils.isEmpty(pChanges)) {
                    finish();
                    return;
                }
                for (PChange pChange : pChanges) {
                    boolean found = false;
                    for (PChange pChangePrev : pChangesPrev) {
                        if (Objects.equals(pChange, pChangePrev)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        String subject = pChange.getSubject();
                        ArrayList<PChange> changes4subject = changes.containsKey(subject) ? changes.get(subject) : new ArrayList<>();
                        changes4subject.add(pChange);
                        changes.put(subject, changes4subject);
                    }
                }
                // step 3
                // creating notifications for existing new changes
                if (changes.isEmpty()) {
                    finish();
                    return;
                }
                long timestamp = System.currentTimeMillis();
                String pref_notify_type = storagePref.get().get(context, "pref_notify_type", Build.VERSION.SDK_INT <= Build.VERSION_CODES.M ? "0" : "1");
                if ("0".equals(pref_notify_type)) {
                    // show single notification per subject that contains all changes related to that subject
                    // best suitable: android <= 6.0 and android == 8.0
                    int index = 0;
                    for (Map.Entry<String, ArrayList<PChange>> change : changes.entrySet()) {
                        final String subject = change.getKey();
                        final ArrayList<PChange> changes4subject = change.getValue();
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
                                text.append("+").append(i + 1).append(" ").append(getActionsLabel(context, i + 1));
                                break;
                            }
                        }
                        addNotification(context, subject, text.toString(), timestamp, index, true);
                        index++;
                    }
                } else {
                    // show notifications per each change that grouped together
                    // best suitable: android >= 7.0 except android == 8.0
                    int size = 0;
                    for (Map.Entry<String, ArrayList<PChange>> change : changes.entrySet()) {
                        size += change.getValue().size();
                    }
                    if (size > 1) {
                        addNotification(context, context.getString(R.string.protocol_changes), String.valueOf(size) + " " + getActionsLabel(context, size), timestamp, 0, true);
                    }
                    for (Map.Entry<String, ArrayList<PChange>> change : changes.entrySet()) {
                        final String subject = change.getKey();
                        final ArrayList<PChange> changes4subject = change.getValue();
                        for (int i = changes4subject.size() - 1; i >= 0; i--) {
                            addNotification(context, subject, change2string(changes4subject.get(i)), timestamp, 0, size == 1);
                        }
                    }
                }
                // step 4
                // there is no step 4
                finish();
            }, throwable -> {
                log.w(TAG, "handle | catch(Thread) | ", throwable.getMessage());
                finish();
            });
        } catch (Exception e) {
            log.w(TAG, "handle | catch | ", e.getMessage());
            finish();
        }
    }

    private void addNotification(@NonNull Context context, String title, String text, long timestamp, int group, boolean isSummary) {
        try {
            thread.run(() -> {
                log.v(TAG, "addNotification | title=", title, " | text=", text.replaceAll("\n", "\\n"), " | timestamp=", timestamp, " | isSummary=", isSummary);
                if (notificationId > Integer.MAX_VALUE - 10) notificationId = 0;
                // prepare intent
                Intent intent = new Intent(context, MainActivity.class);
                intent.addFlags(App.intentFlagRestart);
                intent.putExtra("action", "protocol_changes");
                PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
                // prepare and send notification
                notifications.get()
                        .init(context)
                        .notify(
                                context,
                                notificationId++,
                                notifications.get().getProtocol(context, title, text, timestamp, group, isSummary, pIntent)
                        );
            }, throwable -> {
                log.w(TAG, "addNotification | catch(Thread) | ", throwable.getMessage());
            });
        } catch (Exception e) {
            log.w(TAG, "addNotification | catch | ", e.getMessage());
        }
    }

    private void finish() {
        try {
            firebasePerformanceProvider.stopTrace(trace);
            thread.run(thread.BACKGROUND, () -> {
                try {
                    log.i(TAG, "Executed");
                    if (requestHandle != null) requestHandle.cancel();
                } catch (Exception e) {
                    log.w(TAG, "finish | catch(Thread) | ", e.getMessage());
                } finally {
                    if (callback != null) {
                        callback.call();
                    }
                }
            });
        } catch (Exception e) {
            log.w(TAG, "finish | catch | ", e.getMessage());
            if (callback != null) {
                callback.call();
            }
        }
    }

    private String change2string(PChange change) {
        StringBuilder text = new StringBuilder();
        text.append(change.getValue())
                .append("/")
                .append(change.getMax())
                .append(" — ")
                .append(change.getName());
        if (change.getCdoitmoDeltaDouble() != null && change.getCdoitmoDeltaDouble() != 0.0) {
            text.append(" (").append(change.getCdoitmoDelta()).append(")");
        }
        return text.toString();
    }

    private String getActionsLabel(@NonNull Context context, int size) {
        StringBuilder text = new StringBuilder();
        switch (size % 100) {
            case 10: case 11: case 12: case 13: case 14: text.append(context.getString(R.string.action_3)); break;
            default:
                switch (size % 10) {
                    case 1: text.append(context.getString(R.string.action_1)); break;
                    case 2: case 3: case 4: text.append(context.getString(R.string.action_2)); break;
                    default: text.append(context.getString(R.string.action_3)); break;
                }
                break;
        }
        return text.toString();
    }
}
