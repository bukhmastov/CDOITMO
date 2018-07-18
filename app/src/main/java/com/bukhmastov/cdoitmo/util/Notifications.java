package com.bukhmastov.cdoitmo.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.bukhmastov.cdoitmo.util.impl.NotificationsImpl;

public interface Notifications {

    // future: replace with DI factory
    Notifications instance = new NotificationsImpl();
    static Notifications instance() {
        return instance;
    }

    Notifications init(@NonNull Context context);

    Notification.Builder get(@NonNull String channel, @NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    @RequiresApi(api = Build.VERSION_CODES.O)
    String getProtocolSound(@NonNull Context context);

    @RequiresApi(api = Build.VERSION_CODES.O)
    boolean getProtocolVibration(@NonNull Context context);

    void notify(@NonNull Context context, int id, @Nullable Notification.Builder notification);
}
