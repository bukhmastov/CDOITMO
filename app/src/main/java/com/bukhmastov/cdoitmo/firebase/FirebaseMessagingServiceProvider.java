package com.bukhmastov.cdoitmo.firebase;

import android.app.PendingIntent;
import android.content.Intent;

import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

public class FirebaseMessagingServiceProvider extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingServiceProvider";
    private static final List<String> TOPICS = new ArrayList<String>() {{
        add("PUSH_MESSAGE");
        add("PUSH_RC");
    }};

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Notifications notifications;
    @Inject
    StoragePref storagePref;

    @Override
    public void onNewToken(String s) {
        subscribeToAllTopics();
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            ensureInjected();
            thread.standalone(() -> {

                logMessage(remoteMessage);

                Map<String, String> data = remoteMessage.getData();
                if (data != null && !data.isEmpty()) {
                    handleData(remoteMessage, data);
                }

                RemoteMessage.Notification notification = remoteMessage.getNotification();
                if (notification != null) {
                    handleNotification(remoteMessage, notification);
                }

            }, throwable -> {
                log.exception(throwable);
            });
        } catch (Throwable e) {
            log.exception(e);
        }
    }

    private void logMessage(RemoteMessage remoteMessage) {
        log.i(TAG, "--- Received FCM message ---");
        if (remoteMessage.getNotification() != null) {
            log.i(TAG, "-- Notification");
            log.i(TAG, "- Title: ", remoteMessage.getNotification().getTitle());
            log.i(TAG, "- Body: ", remoteMessage.getNotification().getBody());
        }
        if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {
            log.i(TAG, "-- Data");
            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                log.i(TAG, "- ", entry.getKey(), " : ", entry.getValue());
            }
        }
        log.i(TAG, "----------------------------");
    }

    private void handleData(RemoteMessage remoteMessage, Map<String, String> data) {
        if (data.containsKey("RC_STATE") && Objects.equals(data.get("RC_STATE"), "STALE")) {
            storagePref.put(getBaseContext(), "pref_remote_config_invalidate", true);
        }
    }

    private void handleNotification(RemoteMessage remoteMessage, RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String text = notification.getBody();
        long timestamp = remoteMessage.getSentTime();
        if (StringUtils.isBlank(title) || StringUtils.isBlank(text)) {
            log.w(TAG, "Got FCM notification message with empty title/text | title=", title, " | text=", text, " | timestamp=", timestamp);
            return;
        }
        log.i(TAG, "Got FCM notification message | title=", title, " | text=", text, " | timestamp=", timestamp);
        // prepare intent
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        // prepare and send notification
        notifications.init(getBaseContext()).notify(
                getBaseContext(),
                -1,
                notifications.getSystem(getBaseContext(), title, text, timestamp, true, pendingIntent)
        );
    }

    private void ensureInjected() {
        if (thread == null) {
            AppComponentProvider.getComponent().inject(this);
        }
    }

    public static void subscribeToAllTopics() {
        FirebaseMessaging instance = FirebaseMessaging.getInstance();
        for (String topic : TOPICS) {
            instance.subscribeToTopic(topic);
        }
    }
}
