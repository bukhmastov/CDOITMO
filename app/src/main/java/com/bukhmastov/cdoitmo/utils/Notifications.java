package com.bukhmastov.cdoitmo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.bukhmastov.cdoitmo.R;

// TODO: android >= 26: play music in the group of notifications only once
public class Notifications extends ContextWrapper {

    public static final class CHANNELS {
        public static final String PROTOCOL = "protocol_changes";
        public static final String SYSTEM = "system";
    }
    private static final long[] VIBRATION_PATTERN = new long[] {0, 1000};
    private NotificationManager manager;

    public Notifications(Context context) {
        super(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNELS.PROTOCOL, getString(R.string.notifications_protocol_changes), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            if (Storage.pref.get(context, "pref_notify_vibrate", true)) {
                channel.enableVibration(true);
                channel.setVibrationPattern(VIBRATION_PATTERN);
            } else {
                channel.enableVibration(false);
            }
            String ringtonePath = Storage.pref.get(context, "pref_notify_sound", "").trim();
            if (!ringtonePath.isEmpty()) {
                channel.setSound(Uri.parse(ringtonePath), null);
            }
            getManager().createNotificationChannel(channel);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNELS.SYSTEM, getString(R.string.notifications_system), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(true);
            if (Storage.pref.get(context, "pref_notify_vibrate", true)) {
                channel.enableVibration(true);
                channel.setVibrationPattern(VIBRATION_PATTERN);
            } else {
                channel.enableVibration(false);
            }
            String ringtonePath = Storage.pref.get(context, "pref_notify_sound", "").trim();
            if (!ringtonePath.isEmpty()) {
                channel.setSound(Uri.parse(ringtonePath), null);
            }
            getManager().createNotificationChannel(channel);
        }
    }

    @SuppressWarnings("deprecation")
    public Notification.Builder get(String channel, Context context, String title, String body, long timestamp, boolean isSummary, PendingIntent pIntent) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channel);
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setStyle(new Notification.BigTextStyle().bigText(body));
        builder.setSmallIcon(R.drawable.cdo);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        builder.setGroup("protocol_" + timestamp);
        builder.setGroupSummary(isSummary);
        builder.setCategory(Notification.CATEGORY_EVENT);
        builder.setWhen(timestamp);
        builder.setShowWhen(true);
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);
        if (pIntent != null) {
            builder.setContentIntent(pIntent);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && isSummary) {
            String ringtonePath = Storage.pref.get(context, "pref_notify_sound").trim();
            if (!ringtonePath.isEmpty()) {
                builder.setSound(Uri.parse(ringtonePath));
            }
            if (Storage.pref.get(context, "pref_notify_vibrate", true)) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            }
        }
        return builder;
    }
    public Notification.Builder getProtocol(Context context, String title, String body, long timestamp, boolean isSummary, PendingIntent pIntent) {
        return get(CHANNELS.PROTOCOL, context, title, body, timestamp, isSummary, pIntent);
    }
    public Notification.Builder getSystem(Context context, String title, String body, long timestamp, boolean isSummary, PendingIntent pIntent) {
        return get(CHANNELS.SYSTEM, context, title, body, timestamp, isSummary, pIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getProtocolSound(Context context) {
        NotificationChannel channel = getManager().getNotificationChannel(CHANNELS.PROTOCOL);
        return channel != null ? (channel.getSound() != null ? channel.getSound().toString() : "") : Storage.pref.get(context, "pref_notify_sound", "");
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean getProtocolVibration(Context context) {
        NotificationChannel channel = getManager().getNotificationChannel(CHANNELS.PROTOCOL);
        return channel != null ? channel.shouldVibrate() : Storage.pref.get(context, "pref_notify_vibrate", true);
    }

    public void notify(int id, Notification.Builder notification) {
        if (notification != null) {
            getManager().notify(id, notification.build());
        }
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
