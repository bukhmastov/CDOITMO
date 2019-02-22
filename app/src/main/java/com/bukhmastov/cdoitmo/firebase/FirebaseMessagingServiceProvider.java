package com.bukhmastov.cdoitmo.firebase;

import android.app.PendingIntent;
import android.content.Intent;

import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import javax.inject.Inject;

public class FirebaseMessagingServiceProvider extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingServiceProvider";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Notifications notifications;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (thread == null) {
            AppComponentProvider.getComponent().inject(this);
        }
        try {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification == null) {
                return;
            }
            thread.standalone(() -> {
                String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
                String text = notification.getBody() == null ? "" : notification.getBody().trim();
                log.v(TAG, "-- Got FCM message --");
                log.v(TAG, "Title: ", title);
                log.v(TAG, "Text: ", text);
                log.v(TAG, "---------------------");
                handleNotification(title, text, remoteMessage.getSentTime());
            }, throwable -> {
                log.exception(throwable);
            });
        } catch (Throwable e) {
            log.exception(e);
        }
    }

    private void handleNotification(String title, String text, long timestamp) {
        if (StringUtils.isBlank(title) || StringUtils.isBlank(text)) {
            log.w(TAG, "Got FCM message with empty title/text | title=", title, " | text=", text);
            return;
        }
        log.v(TAG, "handleNotification | title=", title, " | text=", text);
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
}
