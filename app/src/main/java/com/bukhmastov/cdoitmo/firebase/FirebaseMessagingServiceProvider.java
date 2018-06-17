package com.bukhmastov.cdoitmo.firebase;

import android.app.PendingIntent;
import android.content.Intent;

import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.Thread;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingServiceProvider extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingServiceProvider";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification == null) return;
            String title = notification.getTitle() == null ? "" : notification.getTitle().trim();
            String text = notification.getBody() == null ? "" : notification.getBody().trim();
            Log.v(TAG, "-- Got FCM message --");
            Log.v(TAG, "Title: ", title);
            Log.v(TAG, "Text: ", text);
            Log.v(TAG, "---------------------");
            handleNotification(title, text, remoteMessage.getSentTime());
        } catch (Throwable e) {
            Log.exception(e);
        }
    }

    private void handleNotification(final String title, final String text, final long timestamp) {
        Thread.run(() -> {
            try {
                if (title.isEmpty() || text.isEmpty()) {
                    Log.w(TAG, "Got FCM message with empty title/text | title=", title, " | text=", text);
                    return;
                }
                Log.v(TAG, "handleNotification | title=", title, " | text=", text);
                // prepare intent
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
                // prepare and send notification
                Notifications notifications = new Notifications(getBaseContext());
                notifications.notify(-1, notifications.getSystem(getBaseContext(), title, text, timestamp, true, pendingIntent));
            } catch (Throwable e) {
                Log.exception(e);
            }
        });
    }
}
