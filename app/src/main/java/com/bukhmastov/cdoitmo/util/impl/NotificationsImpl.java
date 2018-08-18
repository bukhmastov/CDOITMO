package com.bukhmastov.cdoitmo.util.impl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Notifications;
import com.bukhmastov.cdoitmo.util.StoragePref;

import javax.inject.Inject;

import dagger.Lazy;

public class NotificationsImpl implements Notifications {

    private static final long[] VIBRATION_PATTERN = new long[] {0, 1000};

    private final static String CHANNEL_PROTOCOL = "protocol_changes";
    private final static String CHANNEL_SYSTEM = "system";

    private NotificationManager manager;
    private NotificationManager getManager(@NonNull Context context) {
        if (manager == null) {
            manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @Inject
    Lazy<Log> log;
    @Inject
    StoragePref storagePref;

    public NotificationsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public Notifications init(@NonNull Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createChannel(context, CHANNEL_PROTOCOL, context.getString(R.string.notifications_protocol_changes));
            createChannel(context, CHANNEL_SYSTEM, context.getString(R.string.notifications_system));
        }
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Notification.Builder get(@NonNull String channel, @NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent) {
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
        builder.setGroup("protocol_" + timestamp + "_" + group);
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
            String ringtonePath = storagePref.get(context, "pref_notify_sound").trim();
            if (!ringtonePath.isEmpty()) {
                builder.setSound(Uri.parse(ringtonePath));
            }
            if (storagePref.get(context, "pref_notify_vibrate", true)) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            }
        }
        return builder;
    }

    @Override
    public Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent) {
        return getProtocol(context, title, body, timestamp, 0, isSummary, pIntent);
    }

    @Override
    public Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent) {
        return get(CHANNEL_PROTOCOL, context, title, body, timestamp, group, isSummary, pIntent);
    }

    @Override
    public Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent) {
        return getSystem(context, title, body, timestamp, 0, isSummary, pIntent);
    }

    @Override
    public Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent) {
        return get(CHANNEL_SYSTEM, context, title, body, timestamp, group, isSummary, pIntent);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getProtocolSound(@NonNull Context context) {
        NotificationChannel channel = getManager(context).getNotificationChannel(CHANNEL_PROTOCOL);
        return channel != null ? (channel.getSound() != null ? channel.getSound().toString() : "") : storagePref.get(context, "pref_notify_sound", "");
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean getProtocolVibration(@NonNull Context context) {
        NotificationChannel channel = getManager(context).getNotificationChannel(CHANNEL_PROTOCOL);
        return channel != null ? channel.shouldVibrate() : storagePref.get(context, "pref_notify_vibrate", true);
    }

    @Override
    public void notify(@NonNull Context context, int id, @Nullable Notification.Builder notification) {
        if (notification != null) {
            getManager(context).notify(id, notification.build());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel(@NonNull final Context context, @NonNull final String name, @NonNull final String title) {
        try {
            NotificationChannel channel = getManager(context).getNotificationChannel(name);
            if (channel == null || channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                channel = new NotificationChannel(name, title, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setShowBadge(true);
                if (storagePref.get(context, "pref_notify_vibrate", true)) {
                    channel.enableVibration(true);
                    channel.setVibrationPattern(VIBRATION_PATTERN);
                } else {
                    channel.enableVibration(false);
                }
                String ringtonePath = storagePref.get(context, "pref_notify_sound", "").trim();
                if (!ringtonePath.isEmpty()) {
                    channel.setSound(Uri.parse(ringtonePath), null);
                }
                getManager(context).createNotificationChannel(channel);
            }
        } catch (Exception e) {
            log.get().exception(e);
        }
    }
}
