package com.bukhmastov.cdoitmo.widget;

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
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.view.View;
import android.widget.RemoteViews;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.PikaActivity;
import com.bukhmastov.cdoitmo.activity.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.object.schedule.Schedule;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleLessonsWidget extends AppWidgetProvider {

    private static final String TAG = "SLWidget";
    public static final String ACTION_WIDGET_OPEN = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN";
    public static final String ACTION_WIDGET_UPDATE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_UPDATE";
    public static final String ACTION_WIDGET_OPEN_CONTROLS = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN_CONTROLS";
    public static final String ACTION_WIDGET_CLOSE_CONTROLS = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CLOSE_CONTROLS";
    public static final String ACTION_WIDGET_CONTROLS_NEXT = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_NEXT";
    public static final String ACTION_WIDGET_CONTROLS_BEFORE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_BEFORE";
    public static final String ACTION_WIDGET_CONTROLS_RESET = "com.bukhmastov.cdoitmo.ACTION_WIDGET_CONTROLS_RESET";

    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NARROW, REGULAR, WIDE})
    private @interface SIZE {}
    private static final int NARROW = 0;
    private static final int REGULAR = 1;
    private static final int WIDE = 2;

    private Client.Request requestHandler = null;

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

    public void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force) {
        updateAppWidget(context, appWidgetManager, appWidgetId, force, false);
    }
    public void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force, final boolean controls) {
        Thread.run(() -> {
            Log.i(TAG, "update | appWidgetId=" + appWidgetId);
            try {
                JSONObject settings = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "settings");
                JSONObject cache = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "cache");
                if (settings == null) {
                    needPreparations(context, appWidgetManager, appWidgetId);
                } else if (cache == null || force) {
                    refresh(context, appWidgetManager, appWidgetId, settings);
                } else {
                    long timestamp = cache.getLong("timestamp");
                    long shift = settings.getInt("updateTime") * 3600000L;
                    if (shift != 0 && timestamp + shift < Time.getCalendar().getTimeInMillis()) {
                        refresh(context, appWidgetManager, appWidgetId, settings);
                    } else {
                        display(context, appWidgetManager, appWidgetId, controls);
                    }
                }
            } catch (Exception e) {
                Log.exception(e);
            }
        });
    }
    public void deleteAppWidget(final Context context, final int appWidgetId) {
        Thread.run(() -> {
            Log.i(TAG, "delete | appWidgetId=" + appWidgetId);
            ScheduleLessonsWidgetStorage.delete(context, appWidgetId);
        });
    }

    private void refresh(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Thread.run(() -> {
            Log.i(TAG, "refresh | appWidgetId=" + appWidgetId);
            try {
                new ScheduleLessons(new Schedule.Handler() {
                    @Override
                    public void onSuccess(final JSONObject json, final boolean fromCache) {
                        Thread.run(() -> {
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("timestamp", Time.getCalendar().getTimeInMillis());
                                jsonObject.put("content", json);
                                ScheduleLessonsWidgetStorage.save(context, appWidgetId, "cache", jsonObject.toString());
                                new ScheduleLessonsAdditionalConverter(context, json, content -> {
                                    ScheduleLessonsWidgetStorage.save(context, appWidgetId, "cache_converted", content.toString());
                                    display(context, appWidgetManager, appWidgetId, false);
                                }).run();
                            } catch (Exception e) {
                                Log.exception(e);
                                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                            }
                        });
                    }
                    @Override
                    public void onFailure(int state) {
                        this.onFailure(0, null, state);
                    }
                    @Override
                    public void onFailure(int statusCode, Client.Headers headers, int state) {
                        //R.string.server_provided_corrupted_json
                        String message;
                        switch (state) {
                            case IfmoRestClient.FAILED_SERVER_ERROR: message = IfmoRestClient.getFailureMessage(context, statusCode); break;
                            case IfmoRestClient.FAILED_CORRUPTED_JSON: message = context.getString(R.string.server_provided_corrupted_json); break;
                            default: message = context.getString(R.string.failed_to_load_schedule); break;
                        }
                        failed(context, appWidgetManager, appWidgetId, settings, message);
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
                Log.exception(e);
                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
            }
        });
    }
    private void progress(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Thread.run(() -> {
            Log.v(TAG, "progress | appWidgetId=" + appWidgetId);
            final @SIZE int size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
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
            layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_loading));
            layout.setInt(R.id.slw_loading_text, "setTextColor", colors.text);
            // установки
            bindOpen(context, appWidgetId, layout);
            appWidgetManager.updateAppWidget(appWidgetId, layout);
        });
    }
    private void display(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean controls) {
        Thread.run(() -> {
            Log.v(TAG, "display | appWidgetId=" + appWidgetId + " | controls=" + (controls ? "true" : "false"));
            JSONObject settings = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "settings");
            JSONObject cache = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "cache");
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
                final @SIZE int size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
                final Colors colors = getColors(settings);
                final JSONObject json = cache.getJSONObject("content");
                final Calendar calendar = Time.getCalendar();
                final int[] shift = getShiftBasedOnTime(context, appWidgetId, settings, calendar);
                if (shift[0] + shift[1] != 0) {
                    calendar.add(Calendar.HOUR, (shift[0] + shift[1]) * 24);
                }
                final int week = Time.getWeek(context, calendar) % 2;
                final RemoteViews layout = new RemoteViews(context.getPackageName(), getViewLayout(size));
                // цвет
                layout.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                layout.setInt(R.id.widget_title, "setTextColor", colors.text);
                layout.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                // заголовки
                layout.setViewVisibility(R.id.widget_title, View.VISIBLE);
                layout.setViewVisibility(R.id.widget_day_title, View.VISIBLE);
                layout.setTextViewText(R.id.widget_title, json.getString("title") + ("room".equals(json.getString("type")) ? " " + context.getString(R.string.room).toLowerCase() : ""));
                layout.setTextViewText(R.id.widget_day_title,
                        (
                                (shift[0] != 0 ? ((shift[0] > 0 ? "+" : "") + String.valueOf(shift[0]) + " ") : "") +
                                (shift[1] != 0 ? ("(" + (shift[1] > 0 ? "+" : "") + String.valueOf(shift[1] + ") ")) : "")
                        ) +
                        Time.getDay(context, calendar.get(Calendar.DAY_OF_WEEK)) +
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
                layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_list));
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
                if (!("settings cannot be null".equals(e.getMessage()) || "cache cannot be null".equals(e.getMessage()))) {
                    Log.exception(e);
                }
                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
            }
        });
    }
    private void failed(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings, final String text) {
        Thread.run(() -> {
            Log.v(TAG, "failed | appWidgetId=" + appWidgetId + " | text=" + text);
            final @SIZE int size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
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
            layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_message));
            layout.setInt(R.id.slw_message_text, "setTextColor", colors.text);
            layout.setTextViewText(R.id.slw_message_text, text);
            layout.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_error_outline, colors.text));
            // установки
            bindOpen(context, appWidgetId, layout);
            appWidgetManager.updateAppWidget(appWidgetId, layout);
        });
    }
    private void needPreparations(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Thread.run(() -> {
            Log.v(TAG, "needPreparations | appWidgetId=" + appWidgetId);
            final @SIZE int size = getSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
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
            layout.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_message));
            layout.setInt(R.id.slw_message_text, "setTextColor", colors.text);
            layout.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_info_outline, colors.text));
            // установки
            bindOpen(context, appWidgetId, layout);
            appWidgetManager.updateAppWidget(appWidgetId, layout);
        });
    }

    private void bindMenu(final Context context, final int appWidgetId, final RemoteViews remoteViews, final @SIZE int size) {
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
        if (size == WIDE) {
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
    private void bindControls(final Context context, final int appWidgetId, final RemoteViews remoteViews, final @SIZE int size) {
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
            case WIDE: break;
        }
    }
    private void bindOpen(final Context context, final int appWidgetId, final RemoteViews remoteViews) {
        Intent intent = new Intent(context, ScheduleLessonsWidget.class);
        intent.setAction(ACTION_WIDGET_OPEN);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_title_container, pIntent);
    }

    private int[] getShiftBasedOnTime(final Context context, final int appWidgetId, JSONObject settings, Calendar calendar) {
        int shift = 0;
        int shiftAutomatic = 0;
        // fetch current shift
        try {
            shift = settings.getInt("shift");
        } catch (Exception e) {
            return new int[] {shift, shiftAutomatic};
        }
        // fetch current auto shift, if enabled
        try {
            if (!settings.has("useShiftAutomatic")) {
                settings.put("useShiftAutomatic", true);
            }
            if (!settings.has("shiftAutomatic")) {
                settings.put("shiftAutomatic", 0);
            }
            if (!settings.getBoolean("useShiftAutomatic")) {
                return new int[] {shift, shiftAutomatic};
            }
            shiftAutomatic = settings.getInt("shiftAutomatic");
        } catch (Exception e) {
            return new int[] {shift, shiftAutomatic};
        }
        // calculate new auto shift from schedule
        try {
            // fetch current schedule
            final JSONObject content = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "cache_converted");
            if (content == null) {
                return new int[] {shift, shiftAutomatic};
            }
            final JSONArray schedule = content.getJSONArray("schedule");
            if (schedule == null) {
                return new int[] {shift, shiftAutomatic};
            }
            // seek for next day that contains following lessons
            // to set new shiftAutomatic variable value
            // seek only for 14 days starting today
            final Calendar seek = (Calendar) calendar.clone();
            final Pattern time = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
            days_loop: for (int day = 0; day < 14; day++) {
                if (day > 0) {
                    seek.add(Calendar.HOUR, 24);
                }
                final int week = Time.getWeek(context, seek) % 2;
                final int weekday = Time.getWeekDay(seek);
                final JSONArray lessons = getLessonsForWeekday(schedule, week, weekday);
                // if this day contains lessons
                if (lessons.length() > 0) {
                    if (day == 0) {
                        // if this day - today
                        // seek for last lesson that contains proper timeEnd value
                        for (int i = lessons.length() - 1; i >= 0 ; i--) {
                            final JSONObject lesson = lessons.getJSONObject(i);
                            final Matcher lessonTimeEnd = time.matcher(lesson.getString("timeEnd"));
                            if (lessonTimeEnd.find()) {
                                Calendar calendarLTE = (Calendar) seek.clone();
                                calendarLTE.set(Calendar.HOUR_OF_DAY, Integer.parseInt(lessonTimeEnd.group(1)));
                                calendarLTE.set(Calendar.MINUTE, Integer.parseInt(lessonTimeEnd.group(2)));
                                calendarLTE.set(Calendar.SECOND, 0);
                                if (calendar.getTimeInMillis() > calendarLTE.getTimeInMillis()) {
                                    // if current timestamp more than lesson end timestamp
                                    // then today's lessons have ended and we should continue seeking next day
                                    continue days_loop;
                                } else {
                                    // if current timestamp less than lesson end timestamp
                                    // then today's lessons still not ended, so current shiftAutomatic should be set to 0
                                    shiftAutomatic = saveShiftAutomatic(context, appWidgetId, settings, shiftAutomatic, day);
                                    break days_loop;
                                }
                            }
                        }
                    } else {
                        // if this day - not today (any other following day)
                        // then we found day that contains lessons and current shiftAutomatic should be set to 'day' variable
                        shiftAutomatic = saveShiftAutomatic(context, appWidgetId, settings, shiftAutomatic, day);
                    }
                    break;
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        return new int[] {shift, shiftAutomatic};
    }
    private int saveShiftAutomatic(final Context context, final int appWidgetId, JSONObject settings, int oldShift, int newShift) {
        int delta = newShift - oldShift;
        oldShift = newShift;
        if (delta != 0) {
            try {
                settings.put("shiftAutomatic", oldShift);
                ScheduleLessonsWidgetStorage.save(context, appWidgetId, "settings", settings.toString());
            } catch (Exception e) {
                // failed to save new shifts, restore to previous ones
                oldShift -= delta;
            }
        }
        return oldShift;
    }

    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        Thread.run(() -> {
            final String action = intent.getAction() != null ? intent.getAction() : "";
            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.v(TAG, "onReceive | action=" + action);
            switch (action) {
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
                    Thread.run(() -> {
                        switch (action) {
                            case ACTION_WIDGET_CONTROLS_NEXT: logStatistic(context, "shift_next"); break;
                            case ACTION_WIDGET_CONTROLS_BEFORE: logStatistic(context, "shift_before"); break;
                            case ACTION_WIDGET_CONTROLS_RESET: logStatistic(context, "shift_reset"); break;
                        }
                        JSONObject settings = ScheduleLessonsWidgetStorage.getJson(context, appWidgetId, "settings");
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
                                ScheduleLessonsWidgetStorage.save(context, appWidgetId, "settings", settings.toString());
                            } catch (JSONException ignore) {
                                // ignore
                            }
                        }
                        display(context, AppWidgetManager.getInstance(context), appWidgetId, !ACTION_WIDGET_CONTROLS_RESET.equals(action));
                    });
                    break;
                }
                case ACTION_WIDGET_OPEN: {
                    Thread.run(() -> {
                        logStatistic(context, "schedule_open");
                        Intent oIntent = new Intent(context, MainActivity.class);
                        oIntent.addFlags(App.intentFlagRestart);
                        oIntent.putExtra("action", "schedule_lessons");
                        try {
                            String settings = ScheduleLessonsWidgetStorage.get(context, appWidgetId, "settings");
                            if (settings != null) {
                                oIntent.putExtra("action_extra", new JSONObject(settings).getString("query"));
                            }
                        } catch (Exception e) {
                            Log.exception(e);
                        }
                        context.startActivity(oIntent);
                    });
                    break;
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
            Log.exception(e);
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

    private int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }
    private @LayoutRes int getViewLayout(final @SIZE int size) {
        switch (size) {
            case WIDE:      return R.layout.widget_schedule_lessons_layout_wide;
            case NARROW:    return R.layout.widget_schedule_lessons_layout_small;
            case REGULAR:
            default:        return R.layout.widget_schedule_lessons_layout;
        }
    }
    private @SIZE int getSize(final Bundle options) {
        final int width = getCellsForSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
        final int height = getCellsForSize(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        if (width > 3) {
           return WIDE;
        } else if (height == 1) {
            return NARROW;
        } else {
            return REGULAR;
        }
    }

    public static JSONArray getLessonsForWeekday(final JSONArray schedule, final int week, final int weekday) throws JSONException {
        JSONArray l = new JSONArray();
        for (int i = 0; i < schedule.length(); i++) {
            final JSONObject day = schedule.getJSONObject(i);
            if (day.has("weekday") && day.getInt("weekday") == weekday) {
                final JSONArray lessons = day.has("lessons") ? day.getJSONArray("lessons") : null;
                if (lessons != null) {
                    for (int j = 0; j < lessons.length(); j++) {
                        final JSONObject lesson = lessons.getJSONObject(j);
                        if (lesson != null) {
                            if (week == -1 || (lesson.getInt("week") == 2 || lesson.getInt("week") == week)) {
                                if (!"reduced".equals(lesson.getString("cdoitmo_type"))) {
                                    l.put(lesson);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        return l;
    }

    private void logStatistic(final Context context, final String info) {
        firebaseAnalyticsProvider.logEvent(
                context,
                FirebaseAnalyticsProvider.Event.WIDGET_USAGE,
                firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.WIDGET_USAGE_INFO, info)
        );
    }
}
