package com.bukhmastov.cdoitmo.firebase;

import android.app.PendingIntent;
import android.content.Intent;

import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.Thread;

import javax.inject.Inject;

public class FirebaseMessagingServiceProvider extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingServiceProvider";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Notifications notifications;

    private void inject() {
        if (thread == null) {
            AppComponentProvider.getComponent().inject(this);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        inject();
        try {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification == null) return;
            String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
            String text = notification.getBody() == null ? "" : notification.getBody().trim();
            log.v(TAG, "-- Got FCM message --");
            log.v(TAG, "Title: ", title);
            log.v(TAG, "Text: ", text);
            log.v(TAG, "---------------------");
            handleNotification(title, text, remoteMessage.getSentTime());
        } catch (Throwable e) {
            log.exception(e);
        }
    }

    private void handleNotification(final String title, final String text, final long timestamp) {
        thread.run(() -> {
            try {
                if (title.isEmpty() || text.isEmpty()) {
                    log.w(TAG, "Got FCM message with empty title/text | title=", title, " | text=", text);
                    return;
                }
                log.v(TAG, "handleNotification | title=", title, " | text=", text);
                // prepare intent
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
                // prepare and send notification
                notifications.init(getBaseContext()).notify(getBaseContext(), -1, notifications.getSystem(getBaseContext(), title, text, timestamp, true, pendingIntent));
            } catch (Throwable e) {
                log.exception(e);
            }
        });
    }
}
