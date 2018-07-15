package com.bukhmastov.cdoitmo.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.bukhmastov.cdoitmo.R;

//TODO interface - impl
public class Notifications extends ContextWrapper {

    public static final class CHANNELS {
        public static final String PROTOCOL = "protocol_changes";
        public static final String SYSTEM = "system";
    }
    private static final long[] VIBRATION_PATTERN = new long[] {0, 1000};
    private NotificationManager manager;

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();

    public Notifications(@NonNull Context context) {
        super(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createChannel(context, CHANNELS.PROTOCOL, getString(R.string.notifications_protocol_changes));
            createChannel(context, CHANNELS.SYSTEM, getString(R.string.notifications_system));
        }
    }

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
    public Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent) {
        return getProtocol(context, title, body, timestamp, 0, isSummary, pIntent);
    }
    public Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent) {
        return get(CHANNELS.PROTOCOL, context, title, body, timestamp, group, isSummary, pIntent);
    }
    public Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent) {
        return getSystem(context, title, body, timestamp, 0, isSummary, pIntent);
    }
    public Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent) {
        return get(CHANNELS.SYSTEM, context, title, body, timestamp, group, isSummary, pIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getProtocolSound(@NonNull Context context) {
        NotificationChannel channel = getManager().getNotificationChannel(CHANNELS.PROTOCOL);
        return channel != null ? (channel.getSound() != null ? channel.getSound().toString() : "") : storagePref.get(context, "pref_notify_sound", "");
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean getProtocolVibration(@NonNull Context context) {
        NotificationChannel channel = getManager().getNotificationChannel(CHANNELS.PROTOCOL);
        return channel != null ? channel.shouldVibrate() : storagePref.get(context, "pref_notify_vibrate", true);
    }

    public void notify(int id, @Nullable Notification.Builder notification) {
        if (notification != null) {
            getManager().notify(id, notification.build());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannel(@NonNull final Context context, @NonNull final String name, @NonNull final String title) {
        try {
            NotificationChannel channel = getManager().getNotificationChannel(name);
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
                getManager().createNotificationChannel(channel);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}
