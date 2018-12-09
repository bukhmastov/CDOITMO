package com.bukhmastov.cdoitmo.util;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public interface Notifications {

    Notifications init(@NonNull Context context);

    Notification.Builder get(@NonNull String channel, @NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getProtocol(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystem(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystemHigh(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, boolean isSummary, @Nullable PendingIntent pIntent);

    Notification.Builder getSystemHigh(@NonNull Context context, @NonNull String title, @NonNull String body, long timestamp, int group, boolean isSummary, @Nullable PendingIntent pIntent);

    @RequiresApi(api = Build.VERSION_CODES.O)
    String getProtocolSound(@NonNull Context context);

    @RequiresApi(api = Build.VERSION_CODES.O)
    boolean getProtocolVibration(@NonNull Context context);

    void notify(@NonNull Context context, int id, @Nullable Notification.Builder notification);
}
