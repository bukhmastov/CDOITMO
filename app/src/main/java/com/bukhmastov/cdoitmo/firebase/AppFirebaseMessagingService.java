package com.bukhmastov.cdoitmo.firebase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AppFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "AppFirebaseMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            String title = remoteMessage.getNotification().getTitle().trim();
            String text = remoteMessage.getNotification().getBody().trim();
            long ts = remoteMessage.getSentTime();
            Log.d(TAG, "-- Got FCM message --");
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Text: " + text);
            Log.d(TAG, "---------------------");
            sendNotification(title, text, ts);
        } catch (Throwable e) {
            Static.error(e);
        }
    }

    private void sendNotification(String title, String text, long ts) throws Throwable {
        if (title.isEmpty() || text.isEmpty()) {
            Log.w(TAG, "Got FCM message with empty title/text | title='" + title + "' | text='" + text + "'");
            return;
        }
        Log.v(TAG, "sendNotification | title=" + title + " | text=" + text);
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification.Builder b = new Notification.Builder(this);
        b.setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text));
        b.setSmallIcon(R.drawable.cdo).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        b.setCategory(Notification.CATEGORY_EVENT);
        b.setWhen(ts);
        b.setShowWhen(true);
        b.setAutoCancel(true);
        b.setContentIntent(pendingIntent);
        String ringtonePath = Storage.pref.get(this, "pref_notify_sound");
        if (!ringtonePath.isEmpty()) b.setSound(Uri.parse(ringtonePath));
        if (Storage.pref.get(this, "pref_notify_vibrate", false)) b.setDefaults(Notification.DEFAULT_VIBRATE);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(-1, b.build());
    }

}
