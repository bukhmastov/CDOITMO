package com.bukhmastov.cdoitmo.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.RemoteViews;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.MainActivity;
import com.bukhmastov.cdoitmo.activities.PikaActivity;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.converters.schedule.lessons.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.models.Client;
import com.bukhmastov.cdoitmo.objects.schedule.Schedule;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class ScheduleLessonsWidget extends AppWidgetProvider {

    private static final String TAG = "SLWidget";
    public static final String ACTION_WIDGET_OPEN = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN";
    public static final String ACTION_WIDGET_UPDATE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_UPDATE";
    public static final String ACTION_WIDGET_OPEN_CONTROLS = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN_CONTROLS";
    public static final String ACTION_WIDGET_CLOSE_CONTROLS = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CLOSE_CONTROLS";
    public static final String ACTION_WIDGET_CONTROLS_NEXT = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_NEXT";
    public static final String ACTION_WIDGET_CONTROLS_BEFORE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_BEFORE";
    public static final String ACTION_WIDGET_CONTROLS_RESET = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_RESET";

    private enum SIZE {NARROW, REGULAR, WIDE}
    private static Client.Request requestHandler = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            deleteAppWidget(context, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle options) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, options);
        display(context, appWidgetManager, appWidgetId, false);
    }

    public static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force) {
        updateAppWidget(context, appWidgetManager, appWidgetId, force, false);
    }
    public static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force, final boolean controls) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "update | appWidgetId=" + appWidgetId);
                try {
                    JSONObject settings = Data.getJson(context, appWidgetId, "settings");
                    JSONObject cache = Data.getJson(context, appWidgetId, "cache");
                    if (settings == null) {
                        needPreparations(context, appWidgetManager, appWidgetId);
                    } else if (cache == null || force) {
                        refresh(context, appWidgetManager, appWidgetId, settings);
                    } else {
                        long timestamp = cache.getLong("timestamp");
                        long shift = settings.getInt("updateTime") * 3600000L;
                        if (shift != 0 && timestamp + shift < Calendar.getInstance().getTimeInMillis()) {
                            refresh(context, appWidgetManager, appWidgetId, settings);
                        } else {
                            display(context, appWidgetManager, appWidgetId, controls);
                        }
                    }
                } catch (Exception e) {
                    Static.error(e);
                }
            }
        });
    }
    public static void deleteAppWidget(final Context context, final int appWidgetId) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "delete | appWidgetId=" + appWidgetId);
                Data.delete(context, appWidgetId, "settings");
                Data.delete(context, appWidgetId, "cache");
                Data.delete(context, appWidgetId, "cache_converted");
            }
        });
    }

    private static void refresh(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "refresh | appWidgetId=" + appWidgetId);
                try {
                    new ScheduleLessons(new Schedule.Handler() {
                        @Override
                        public void onSuccess(final JSONObject json, final boolean fromCache) {
                            Static.T.runThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                        jsonObject.put("content", json);
                                        Data.save(context, appWidgetId, "cache", jsonObject.toString());
                                        new ScheduleLessonsAdditionalConverter(context, json, new ScheduleLessonsAdditionalConverter.response() {
                                            @Override
                                            public void finish(final JSONObject content) {
                                                Data.save(context, appWidgetId, "cache_converted", content.toString());
                                                display(context, appWidgetManager, appWidgetId, false);
                                            }
                                        }).run();
                                    } catch (Exception e) {
                                        Static.error(e);
                                        failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                                    }
                                }
                            });
                        }
                        @Override
                        public void onFailure(int state) {
                            this.onFailure(0, null, state);
                        }
                        @Override
                        public void onFailure(int statusCode, Client.Headers headers, int state) {
                            failed(context, appWidgetManager, appWidgetId, settings, state == IfmoRestClient.FAILED_SERVER_ERROR ? IfmoRestClient.getFailureMessage(context, statusCode) : context.getString(R.string.failed_to_load_schedule));
                        }
                        @Override
                        public void onProgress(int state) {
                            progress(context, appWidgetManager, appWidgetId, settings);
                        }
                        @Override
                        public void onNewRequest(Client.Request request) {
                            requestHandler = request;
                        }
                        @Override
                        public void onCancelRequest() {
                            if (requestHandler != null) {
                                requestHandler.cancel();
                            }
                        }
                    }).search(context, settings.getString("query"), 0, false, false);
                } catch (Exception e) {
                    Static.error(e);
                    failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
                }
            }
        });
    }
    private static void progress(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "progress | appWidgetId=" + appWidgetId);
                final SIZE size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
                final Colors colors = getColors(settings);
                final RemoteViews layout = new RemoteViews(context.getPackageName(), getViewLayout(size));
                // цвет
                layout.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_title, "setTextColor", colors.text);
                layout.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                // заголовки
                layout.setViewVisibility(R.id.widget_title, View.VISIBLE);
                layout.setViewVisibility(R.id.widget_day_title, View.GONE);
                layout.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                // кнопки управления
                switch (size) {
                    case REGULAR:
                    case NARROW: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_controls_open_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_controls_close_container, View.GONE);
                        break;
                    }
                    case WIDE: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_before_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_reset_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_next_container, View.GONE);
                        break;
                    }
                }
                // панель управления
                layout.setViewVisibility(R.id.widget_controls, View.GONE);
                // контент
                layout.removeAllViews(R.id.widget_container);
                layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_loading));
                layout.setInt(R.id.slw_loading_text, "setTextColor", colors.text);
                // установки
                bindOpen(context, appWidgetId, layout);
                appWidgetManager.updateAppWidget(appWidgetId, layout);
            }
        });
    }
    private static void display(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean controls) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display | appWidgetId=" + appWidgetId + " | controls=" + (controls ? "true" : "false"));
                JSONObject settings = Data.getJson(context, appWidgetId, "settings");
                JSONObject cache = Data.getJson(context, appWidgetId, "cache");
                try {
                    if (settings == null) {
                        needPreparations(context, appWidgetManager, appWidgetId);
                        return;
                    }
                    if (cache == null) {
                        refresh(context, appWidgetManager, appWidgetId, settings);
                        return;
                    }
                    if (!settings.has("shift")) {
                        settings.put("shift", 0);
                    }
                    final SIZE size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
                    final Colors colors = getColors(settings);
                    final JSONObject json = cache.getJSONObject("content");
                    final int shift = settings.getInt("shift");
                    final Calendar calendar = Calendar.getInstance(Locale.GERMANY);
                    if (shift != 0) {
                        calendar.add(Calendar.HOUR, shift * 24);
                    }
                    final int week = Static.getWeek(context, calendar) % 2;
                    final RemoteViews layout = new RemoteViews(context.getPackageName(), getViewLayout(size));
                    // цвет
                    layout.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                    layout.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                    layout.setInt(R.id.widget_title, "setTextColor", colors.text);
                    layout.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                    // заголовки
                    layout.setViewVisibility(R.id.widget_title, View.VISIBLE);
                    layout.setViewVisibility(R.id.widget_day_title, View.VISIBLE);
                    layout.setTextViewText(R.id.widget_title, json.getString("title") + (Objects.equals(json.getString("type"), "room") ? " " + context.getString(R.string.room).toLowerCase() : ""));
                    layout.setTextViewText(R.id.widget_day_title,
                            (shift != 0 ? (shift > 0 ? "+" : "") + String.valueOf(shift) + " " : "") +
                            Static.getDay(context, calendar.get(Calendar.DAY_OF_WEEK)) +
                            (week == 0 ? " (" + context.getString(R.string.tab_even) + ")" : (week == 1 ? " (" + context.getString(R.string.tab_odd) + ")" : ""))
                    );
                    // кнопки управления
                    switch (size) {
                        case REGULAR: {
                            layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                            layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                            if (controls) {
                                layout.setViewVisibility(R.id.widget_controls_open_container, View.GONE);
                                layout.setViewVisibility(R.id.widget_controls_close_container, View.VISIBLE);
                                layout.setImageViewBitmap(R.id.widget_controls_close_button, getBitmap(context, R.drawable.ic_widget_close, colors.text));
                            } else {
                                layout.setViewVisibility(R.id.widget_controls_open_container, View.VISIBLE);
                                layout.setViewVisibility(R.id.widget_controls_close_container, View.GONE);
                                layout.setImageViewBitmap(R.id.widget_controls_open_button, getBitmap(context, R.drawable.ic_widget_expand, colors.text));
                            }
                            break;
                        }
                        case NARROW: {
                            layout.setViewVisibility(R.id.widget_refresh_container, View.GONE);
                            if (controls) {
                                layout.setViewVisibility(R.id.widget_controls_open_container, View.GONE);
                                layout.setViewVisibility(R.id.widget_controls_close_container, View.VISIBLE);
                                layout.setImageViewBitmap(R.id.widget_controls_close_button, getBitmap(context, R.drawable.ic_widget_close, colors.text));
                            } else {
                                layout.setViewVisibility(R.id.widget_controls_open_container, View.VISIBLE);
                                layout.setViewVisibility(R.id.widget_controls_close_container, View.GONE);
                                layout.setImageViewBitmap(R.id.widget_controls_open_button, getBitmap(context, R.drawable.ic_widget_expand, colors.text));
                            }
                            break;
                        }
                        case WIDE: {
                            layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                            layout.setViewVisibility(R.id.widget_before_container, View.VISIBLE);
                            layout.setViewVisibility(R.id.widget_reset_container, View.VISIBLE);
                            layout.setViewVisibility(R.id.widget_next_container, View.VISIBLE);
                            layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                            layout.setImageViewBitmap(R.id.widget_before_button, getBitmap(context, R.drawable.ic_widget_before, colors.text));
                            layout.setImageViewBitmap(R.id.widget_reset_button, getBitmap(context, R.drawable.ic_widget_reset, colors.text));
                            layout.setImageViewBitmap(R.id.widget_next_button, getBitmap(context, R.drawable.ic_widget_next, colors.text));
                            break;
                        }
                    }
                    bindMenu(context, appWidgetId, layout, size);
                    // панель управления
                    switch (size) {
                        case REGULAR: {
                            if (controls) {
                                layout.setViewVisibility(R.id.widget_controls, View.VISIBLE);
                                layout.setInt(R.id.widget_controls, "setBackgroundColor", colors.background);
                                layout.setImageViewBitmap(R.id.widget_next_button, getBitmap(context, R.drawable.ic_widget_next, colors.text));
                                layout.setImageViewBitmap(R.id.widget_before_button, getBitmap(context, R.drawable.ic_widget_before, colors.text));
                                layout.setImageViewBitmap(R.id.widget_reset_button, getBitmap(context, R.drawable.ic_widget_reset, colors.text));
                                bindControls(context, appWidgetId, layout, size);
                            } else {
                                layout.setViewVisibility(R.id.widget_controls, View.GONE);
                            }
                            break;
                        }
                        case NARROW: {
                            if (controls) {
                                layout.setViewVisibility(R.id.widget_controls, View.VISIBLE);
                                layout.setInt(R.id.widget_controls, "setBackgroundColor", colors.background);
                                layout.setImageViewBitmap(R.id.widget_before_button, getBitmap(context, R.drawable.ic_widget_before, colors.text));
                                layout.setImageViewBitmap(R.id.widget_refresh_control_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                                layout.setImageViewBitmap(R.id.widget_next_button, getBitmap(context, R.drawable.ic_widget_next, colors.text));
                                bindControls(context, appWidgetId, layout, size);
                            } else {
                                layout.setViewVisibility(R.id.widget_controls, View.GONE);
                            }
                            break;
                        }
                        case WIDE: {
                            layout.setViewVisibility(R.id.widget_controls, View.GONE);
                            break;
                        }
                    }
                    // контент
                    layout.removeAllViews(R.id.widget_container);
                    layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_list));
                    // установки
                    bindOpen(context, appWidgetId, layout);
                    // список расписания
                    Intent intent = new Intent(context, ScheduleLessonsWidgetService.class);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                    layout.setRemoteAdapter(R.id.slw_day_schedule, intent);
                    appWidgetManager.updateAppWidget(appWidgetId, layout);
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.slw_day_schedule);
                } catch (Exception e) {
                    if (!(Objects.equals(e.getMessage(), "settings cannot be null") || Objects.equals(e.getMessage(), "cache cannot be null"))) {
                        Static.error(e);
                    }
                    failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                }
            }
        });
    }
    private static void failed(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings, final String text) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failed | appWidgetId=" + appWidgetId + " | text=" + text);
                final SIZE size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
                final Colors colors = getColors(settings);
                final RemoteViews layout = new RemoteViews(context.getPackageName(), getViewLayout(size));
                // цвет
                layout.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_title, "setTextColor", colors.text);
                layout.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                // заголовки
                layout.setViewVisibility(R.id.widget_title, View.VISIBLE);
                layout.setViewVisibility(R.id.widget_day_title, View.GONE);
                layout.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                // кнопки управления
                switch (size) {
                    case REGULAR:
                    case NARROW: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                        layout.setViewVisibility(R.id.widget_controls_open_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_controls_close_container, View.GONE);
                        layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                        break;
                    }
                    case WIDE: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                        layout.setViewVisibility(R.id.widget_before_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_reset_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_next_container, View.GONE);
                        layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                        break;
                    }
                }
                bindMenu(context, appWidgetId, layout, size);
                // панель управления
                layout.setViewVisibility(R.id.widget_controls, View.GONE);
                // контент
                layout.removeAllViews(R.id.widget_container);
                layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                layout.setInt(R.id.slw_message_text, "setTextColor", colors.text);
                layout.setTextViewText(R.id.slw_message_text, text);
                layout.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_error_outline, colors.text));
                // установки
                bindOpen(context, appWidgetId, layout);
                appWidgetManager.updateAppWidget(appWidgetId, layout);
            }
        });
    }
    private static void needPreparations(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "needPreparations | appWidgetId=" + appWidgetId);
                final SIZE size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
                final Colors colors = getColors();
                final RemoteViews layout = new RemoteViews(context.getPackageName(), getViewLayout(size));
                // цвет
                layout.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_title, "setTextColor", colors.text);
                layout.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                // заголовки
                layout.setViewVisibility(R.id.widget_title, View.VISIBLE);
                layout.setViewVisibility(R.id.widget_day_title, View.GONE);
                layout.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                // кнопки управления
                switch (size) {
                    case REGULAR:
                    case NARROW: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                        layout.setViewVisibility(R.id.widget_controls_open_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_controls_close_container, View.GONE);
                        layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                        break;
                    }
                    case WIDE: {
                        layout.setViewVisibility(R.id.widget_refresh_container, View.VISIBLE);
                        layout.setViewVisibility(R.id.widget_before_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_reset_container, View.GONE);
                        layout.setViewVisibility(R.id.widget_next_container, View.GONE);
                        layout.setImageViewBitmap(R.id.widget_refresh_button, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                        break;
                    }
                }
                bindMenu(context, appWidgetId, layout, size);
                // панель управления
                layout.setViewVisibility(R.id.widget_controls, View.GONE);
                // контент
                layout.removeAllViews(R.id.widget_container);
                layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                layout.setInt(R.id.slw_message_text, "setTextColor", colors.text);
                layout.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_info_outline, colors.text));
                // установки
                bindOpen(context, appWidgetId, layout);
                appWidgetManager.updateAppWidget(appWidgetId, layout);
            }
        });
    }

    private static void bindMenu(final Context context, final int appWidgetId, final RemoteViews remoteViews, final SIZE size) {
        Intent intent;
        // refresh
        intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        // open
        intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_OPEN_CONTROLS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_controls_open_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        // close
        intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_CLOSE_CONTROLS);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_controls_close_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        if (size == SIZE.WIDE) {
            // before
            intent = new Intent(context, ScheduleLessonsWidget.class);
            intent.setAction(ACTION_WIDGET_CONTROLS_BEFORE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setOnClickPendingIntent(R.id.widget_before_container, PendingIntent.getBroadcast(context, 0, intent, 0));
            // reset
            intent = new Intent(context, ScheduleLessonsWidget.class);
            intent.setAction(ACTION_WIDGET_CONTROLS_RESET);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setOnClickPendingIntent(R.id.widget_reset_container, PendingIntent.getBroadcast(context, 0, intent, 0));
            // next
            intent = new Intent(context, ScheduleLessonsWidget.class);
            intent.setAction(ACTION_WIDGET_CONTROLS_NEXT);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            remoteViews.setOnClickPendingIntent(R.id.widget_next_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        }
    }
    private static void bindControls(final Context context, final int appWidgetId, final RemoteViews remoteViews, final SIZE size) {
        Intent intent;
        // next
        intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_CONTROLS_NEXT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_next_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        // before
        intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_CONTROLS_BEFORE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_before_container, PendingIntent.getBroadcast(context, 0, intent, 0));
        switch (size) {
            case REGULAR: {
                // reset
                intent = new Intent(context, ScheduleLessonsWidget.class);
                intent.setAction(ACTION_WIDGET_CONTROLS_RESET);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(R.id.widget_reset_container, PendingIntent.getBroadcast(context, 0, intent, 0));
                break;
            }
            case NARROW: {
                // refresh
                intent = new Intent(context, ScheduleLessonsWidget.class);
                intent.setAction(ACTION_WIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(R.id.widget_refresh_control_container, PendingIntent.getBroadcast(context, 0, intent, 0));
                break;
            }
        }
    }
    private static void bindOpen(final Context context, final int appWidgetId, final RemoteViews remoteViews) {
        Intent intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_OPEN);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_title_container, pIntent);
    }

    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                final String action = intent.getAction();
                final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                Log.v(TAG, "onReceive | action=" + action);
                switch (action != null ? action : "") {
                    case ACTION_WIDGET_UPDATE: {
                        logStatistic(context, "force_update");
                        updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, true);
                        break;
                    }
                    case ACTION_WIDGET_OPEN_CONTROLS: {
                        logStatistic(context, "controls_open");
                        display(context, AppWidgetManager.getInstance(context), appWidgetId, true);
                        break;
                    }
                    case ACTION_WIDGET_CLOSE_CONTROLS: {
                        logStatistic(context, "controls_close");
                        display(context, AppWidgetManager.getInstance(context), appWidgetId, false);
                        break;
                    }
                    case ACTION_WIDGET_CONTROLS_NEXT:
                    case ACTION_WIDGET_CONTROLS_BEFORE:
                    case ACTION_WIDGET_CONTROLS_RESET: {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                switch (action) {
                                    case ACTION_WIDGET_CONTROLS_NEXT: logStatistic(context, "shift_next"); break;
                                    case ACTION_WIDGET_CONTROLS_BEFORE: logStatistic(context, "shift_before"); break;
                                    case ACTION_WIDGET_CONTROLS_RESET: logStatistic(context, "shift_reset"); break;
                                }
                                JSONObject settings = Data.getJson(context, appWidgetId, "settings");
                                if (settings != null) {
                                    int shift;
                                    try {
                                        shift = settings.getInt("shift");
                                    } catch (JSONException e) {
                                        shift = 0;
                                    }
                                    switch (action) {
                                        case ACTION_WIDGET_CONTROLS_NEXT: shift++; break;
                                        case ACTION_WIDGET_CONTROLS_BEFORE: shift--; break;
                                        case ACTION_WIDGET_CONTROLS_RESET: shift = 0; break;
                                    }
                                    if (shift > 180 || shift < -180) {
                                        shift = 0;
                                        context.startActivity(new Intent(context, PikaActivity.class));
                                    }
                                    try {
                                        settings.put("shift", shift);
                                        Data.save(context, appWidgetId, "settings", settings.toString());
                                    } catch (JSONException ignore) {
                                        // ignore
                                    }
                                }
                                display(context, AppWidgetManager.getInstance(context), appWidgetId, !Objects.equals(action, ACTION_WIDGET_CONTROLS_RESET));
                            }
                        });
                        break;
                    }
                    case ACTION_WIDGET_OPEN: {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                logStatistic(context, "schedule_open");
                                Intent oIntent = new Intent(context, MainActivity.class);
                                oIntent.addFlags(Static.intentFlagRestart);
                                oIntent.putExtra("action", "schedule_lessons");
                                try {
                                    String settings = Data.get(context, appWidgetId, "settings");
                                    if (settings != null) {
                                        oIntent.putExtra("action_extra", new JSONObject(settings).getString("query"));
                                    }
                                } catch (Exception e) {
                                    Static.error(e);
                                }
                                context.startActivity(oIntent);
                            }
                        });
                        break;
                    }
                }
            }
        });
    }

    public static class Colors {
        int background;
        int text;
    }
    public static Colors getColors(JSONObject settings) {
        try {
            JSONObject theme = settings.getJSONObject("theme");
            Colors colors = new Colors();
            colors.text = Color.parseColor(theme.getString("text"));
            colors.background = Color.parseColor(theme.getString("background"));
            colors.background = Color.argb(theme.getInt("opacity"), Color.red(colors.background), Color.green(colors.background), Color.blue(colors.background));
            return colors;
        } catch (Exception e) {
            return getColors();
        }
    }
    public static Colors getColors() {
        Colors colors = new Colors();
        colors.text = Color.parseColor(ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.text);
        colors.background = Color.parseColor(ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.background);
        colors.background = Color.argb(ScheduleLessonsWidgetConfigureActivity.Default.Theme.Dark.opacity, Color.red(colors.background), Color.green(colors.background), Color.blue(colors.background));
        return colors;
    }
    public static Bitmap getBitmap(Context context, @DrawableRes int drawableRes, int color) {
        try {
            Bitmap b = res2Bitmap(context, drawableRes);
            Bitmap bitmap = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(b, 0, 0, paint);
            return bitmap;
        } catch (Exception e) {
            Static.error(e);
            return null;
        }
    }
    public static Bitmap res2Bitmap(Context context, @DrawableRes int drawableRes) throws Exception {
        Drawable drawable = context.getResources().getDrawable(drawableRes, context.getTheme());
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }
    private static @LayoutRes int getViewLayout(final SIZE size) {
        switch (size) {
            case WIDE:      return R.layout.schedule_lessons_widget_wide;
            case NARROW:    return R.layout.schedule_lessons_widget_small;
            case REGULAR:
            default:        return R.layout.schedule_lessons_widget;
        }
    }
    private static SIZE getSize(final Bundle options) {
        final int width = getCellsForSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
        final int height = getCellsForSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        if (width > 3) {
           return SIZE.WIDE;
        } else if (height == 1) {
            return SIZE.NARROW;
        } else {
            return SIZE.REGULAR;
        }
    }

    public static class Data {
        public static String get(Context context, int appWidgetId, String type) {
            Log.v(TAG, "get | appWidgetId=" + appWidgetId + " | type=" + type);
            String pref;
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.w(TAG, "get | prevented due to invalid appwidget id");
                pref = "";
            } else {
                pref = Storage.file.general.get(context, "widget_schedule_lessons#" + appWidgetId + "#" + type).trim();
            }
            return pref.isEmpty() ? null : pref;
        }
        public static JSONObject getJson(Context context, int appWidgetId, String type) {
            Log.v(TAG, "getJson | appWidgetId=" + appWidgetId + " | type=" + type);
            JSONObject pref;
            try {
                String tmp = get(context, appWidgetId, type);
                if (tmp == null) throw new NullPointerException(type + " is null");
                pref = new JSONObject(tmp);
            } catch (Exception e) {
                pref = null;
            }
            return pref;
        }
        public static void save(Context context, int appWidgetId, String type, String text) {
            Log.v(TAG, "save | appWidgetId=" + appWidgetId + " | type=" + type);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.w(TAG, "save | prevented due to invalid appwidget id");
                return;
            }
            Storage.file.general.put(context, "widget_schedule_lessons#" + appWidgetId + "#" + type, text);
        }
        public static void delete(Context context, int appWidgetId, String type) {
            Log.v(TAG, "delete | appWidgetId=" + appWidgetId + " | type=" + type);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.w(TAG, "delete | prevented due to invalid appwidget id");
                return;
            }
            Storage.file.general.delete(context, "widget_schedule_lessons#" + appWidgetId + "#" + type);
        }
    }

    private static void logStatistic(final Context context, final String info) {
        FirebaseAnalyticsProvider.logEvent(
                context,
                FirebaseAnalyticsProvider.Event.WIDGET_USAGE,
                FirebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.WIDGET_USAGE_INFO, info)
        );
    }
}
