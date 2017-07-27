package com.bukhmastov.cdoitmo.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.RemoteViews;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;
import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

public class ScheduleLessonsWidget extends AppWidgetProvider {

    private static final String TAG = "SLWidget";
    public static final String ACTION_WIDGET_UPDATE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_UPDATE";
    public static final String ACTION_WIDGET_OPEN = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN";

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

    public static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "update | appWidgetId=" + appWidgetId);
                try {
                    JSONObject settings = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "settings");
                    JSONObject cache = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "cache");
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
                            display(context, appWidgetManager, appWidgetId);
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
                ScheduleLessonsWidgetConfigureActivity.deletePref(context, appWidgetId, "settings");
                ScheduleLessonsWidgetConfigureActivity.deletePref(context, appWidgetId, "cache");
            }
        });
    }

    private static void refresh(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "refresh | appWidgetId=" + appWidgetId);
                ScheduleLessons scheduleLessons = new ScheduleLessons(context);
                scheduleLessons.setHandler(new ScheduleLessons.response(){
                    @Override
                    public void onProgress(int state) {
                        progress(context, appWidgetManager, appWidgetId, settings);
                    }
                    @Override
                    public void onFailure(int state) {
                        switch (state) {
                            case IfmoRestClient.FAILED_OFFLINE:
                            case IfmoRestClient.FAILED_TRY_AGAIN:
                            case ScheduleLessons.FAILED_LOAD:
                                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
                                break;
                        }
                    }
                    @Override
                    public void onSuccess(JSONObject json) {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                            jsonObject.put("content", json);
                            ScheduleLessonsWidgetConfigureActivity.savePref(context, appWidgetId, "cache", jsonObject.toString());
                            display(context, appWidgetManager, appWidgetId);
                        } catch (Exception e){
                            Static.error(e);
                            failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                        }
                    }
                    @Override
                    public void onNewHandle(RequestHandle requestHandle) {}
                });
                try {
                    scheduleLessons.search(settings.getString("query"), 0, false, false);
                } catch (JSONException e) {
                    Static.error(e);
                    failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
                }
            }
        });
    }
    private static void progress(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "progress | appWidgetId=" + appWidgetId);
                int[] colors = getColors(settings);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title_layout, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title, "setTextColor", colors[0]);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_loading));
                views.setInt(R.id.slw_loading_text, "setTextColor", colors[0]);
                bindOpen(context, appWidgetId, views);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }
    private static void display(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display | appWidgetId=" + appWidgetId);
                JSONObject settings = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "settings");
                JSONObject cache = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "cache");
                try {
                    if (settings == null) throw new NullPointerException("settings cannot be null");
                    if (cache == null) throw new NullPointerException("cache cannot be null");
                    int[] colors = getColors(settings);
                    boolean theme = getTheme(settings);
                    JSONObject json = cache.getJSONObject("content");
                    final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                    views.setInt(R.id.widget_content, "setBackgroundColor", colors[1]);
                    views.setInt(R.id.widget_title_layout, "setBackgroundColor", colors[1]);
                    views.setInt(R.id.widget_title, "setTextColor", colors[0]);
                    views.setTextViewText(R.id.widget_title, (Objects.equals(json.getString("type"), "room") ? context.getString(R.string.room) + " " : "") + json.getString("label"));
                    views.setImageViewResource(R.id.widget_status, theme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
                    views.removeAllViews(R.id.widget_container);
                    views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_list));
                    String title = "";
                    switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)){
                        case Calendar.MONDAY: title = context.getString(R.string.monday).toUpperCase(); break;
                        case Calendar.TUESDAY: title = context.getString(R.string.tuesday).toUpperCase(); break;
                        case Calendar.WEDNESDAY: title = context.getString(R.string.wednesday).toUpperCase(); break;
                        case Calendar.THURSDAY: title = context.getString(R.string.thursday).toUpperCase(); break;
                        case Calendar.FRIDAY: title = context.getString(R.string.friday).toUpperCase(); break;
                        case Calendar.SATURDAY: title = context.getString(R.string.saturday).toUpperCase(); break;
                        case Calendar.SUNDAY: title = context.getString(R.string.sunday).toUpperCase(); break;
                    }
                    int week = Static.getWeek(context) % 2;
                    views.setTextViewText(R.id.slw_day_title, title + (week == 0 ? " (" + context.getString(R.string.tab_even) + ")" : (week == 1 ? " (" + context.getString(R.string.tab_odd) + ")" : "")).toUpperCase());
                    views.setInt(R.id.slw_day_title, "setTextColor", colors[0]);
                    views.setInt(R.id.slw_day_title, "setBackgroundColor", Color.argb(80, Color.red(colors[1]), Color.green(colors[1]), Color.blue(colors[1])));
                    new ScheduleLessonsAdditionalConverter(context, new ScheduleLessonsAdditionalConverter.response() {
                        @Override
                        public void finish(JSONObject content) {
                            ScheduleLessonsWidgetConfigureActivity.savePref(context, appWidgetId, "cache_converted", content.toString());
                            Intent adapter = new Intent(context, ScheduleLessonsWidgetService.class);
                            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                            adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
                            views.setRemoteAdapter(R.id.slw_day_schedule, adapter);
                            bindRefresh(context, appWidgetId, views);
                            bindOpen(context, appWidgetId, views);
                            appWidgetManager.updateAppWidget(appWidgetId, views);
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.slw_day_schedule);
                        }
                    }).execute(cache.getJSONObject("content"));
                } catch (Exception e) {
                    Static.error(e);
                    if (Objects.equals(e.getMessage(), "settings cannot be null")) {
                        needPreparations(context, appWidgetManager, appWidgetId);
                    } else {
                        failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                    }
                }
            }
        });
    }
    private static void failed(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings, final String text) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failed | appWidgetId=" + appWidgetId + " | text=" + text);
                int[] colors = getColors(settings);
                boolean theme = getTheme(settings);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title_layout, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title, "setTextColor", colors[0]);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.setImageViewResource(R.id.widget_status, theme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                views.setInt(R.id.slw_message_text, "setTextColor", colors[0]);
                views.setTextViewText(R.id.slw_message_text, text);
                views.setImageViewResource(R.id.slw_message_icon, theme ? R.drawable.ic_widget_error_outline : R.drawable.ic_widget_info_outline_dark);
                bindRefresh(context, appWidgetId, views);
                bindOpen(context, appWidgetId, views);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }
    private static void needPreparations(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "needPreparations | appWidgetId=" + appWidgetId);
                int[] colors = getColors(context);
                boolean theme = getTheme(context);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title_layout, "setBackgroundColor", colors[1]);
                views.setInt(R.id.widget_title, "setTextColor", colors[0]);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.setImageViewResource(R.id.widget_status, theme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                views.setInt(R.id.slw_message_text, "setTextColor", colors[0]);
                views.setImageViewResource(R.id.slw_message_icon, theme ? R.drawable.ic_widget_info_outline : R.drawable.ic_widget_info_outline_dark);
                bindRefresh(context, appWidgetId, views);
                bindOpen(context, appWidgetId, views);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }

    private static int[] getColors(Context context) {
        return getColors(getTheme(context));
    }
    private static int[] getColors(JSONObject settings) {
        return getColors(getTheme(settings));
    }
    private static int[] getColors(boolean theme) {
        int[] colors = new int[2];
        if (theme) {
            colors[0] = Color.parseColor("#FFFFFF");
            colors[1] = Color.parseColor("#000000");
        } else {
            colors[0] = Color.parseColor("#000000");
            colors[1] = Color.parseColor("#FFFFFF");
        }
        colors[1] = Color.argb(150, Color.red(colors[1]), Color.green(colors[1]), Color.blue(colors[1]));
        return colors;
    }
    private static boolean getTheme(Context context) {
        return Storage.pref.get(context, "pref_dark_theme", false);
    }
    private static boolean getTheme(JSONObject settings) {
        boolean darkTheme;
        try {
            darkTheme = settings.getBoolean("darkTheme");
        } catch (JSONException e) {
            darkTheme = true;
        }
        return darkTheme;
    }

    private static void bindRefresh(final Context context, final int appWidgetId, final RemoteViews remoteViews) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent adapter = new Intent(context, ScheduleLessonsWidget.class);
                adapter.setAction(ACTION_WIDGET_UPDATE);
                adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(R.id.widget_status, PendingIntent.getBroadcast(context, 0, adapter, 0));
            }
        });
    }
    private static void bindOpen(final Context context, final int appWidgetId, final RemoteViews remoteViews) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent adapter = new Intent(context, ScheduleLessonsWidget.class);
                adapter.setAction(ACTION_WIDGET_OPEN);
                adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
                PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, adapter, 0);
                remoteViews.setOnClickPendingIntent(R.id.widget_content, pIntent);
            }
        });
    }

    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "onReceive | action=" + intent.getAction());
                switch (intent.getAction()) {
                    case ACTION_WIDGET_UPDATE: {
                        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        updateAppWidget(context, appWidgetManager, appWidgetId, true);
                        break;
                    }
                    case ACTION_WIDGET_OPEN: {
                        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                        Intent oIntent = new Intent(context, SplashActivity.class);
                        oIntent.addFlags(Static.intentFlagRestart);
                        oIntent.putExtra("action", "schedule_lessons");
                        try {
                            String settings = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "settings");
                            if (settings == null) throw new NullPointerException("settings is null");
                            oIntent.putExtra("action_extra", new JSONObject(settings).getString("query"));
                        } catch (Exception e) {
                            Static.error(e);
                        }
                        context.startActivity(oIntent);
                        break;
                    }
                }
            }
        });
    }
}
