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
import android.support.annotation.DrawableRes;
import android.view.View;
import android.widget.RemoteViews;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.activities.SplashActivity;
import com.bukhmastov.cdoitmo.converters.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
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
                ScheduleLessonsWidgetConfigureActivity.deletePref(context, appWidgetId, "cache_converted");
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
                            case ScheduleLessons.FAILED_EMPTY_QUERY:
                                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
                                break;
                        }
                    }
                    @Override
                    public void onSuccess(final JSONObject json) {
                        Static.T.runThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("timestamp", Calendar.getInstance().getTimeInMillis());
                                    jsonObject.put("content", json);
                                    ScheduleLessonsWidgetConfigureActivity.savePref(context, appWidgetId, "cache", jsonObject.toString());
                                    display(context, appWidgetManager, appWidgetId);
                                } catch (Exception e) {
                                    Static.error(e);
                                    failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                                }
                            }
                        });
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "progress | appWidgetId=" + appWidgetId);
                Colors colors = getColors(settings);
                final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_title, "setTextColor", colors.text);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_loading));
                views.setInt(R.id.slw_loading_text, "setTextColor", colors.text);
                bindOpen(context, appWidgetId, views);
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                });
            }
        });
    }
    private static void display(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "display | appWidgetId=" + appWidgetId);
                JSONObject settings = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "settings");
                JSONObject cache = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "cache");
                try {
                    if (settings == null) throw new NullPointerException("settings cannot be null");
                    if (cache == null) throw new NullPointerException("cache cannot be null");
                    final Colors colors = getColors(settings);
                    JSONObject json = cache.getJSONObject("content");
                    final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                    views.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                    views.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                    views.setInt(R.id.widget_title, "setTextColor", colors.text);
                    views.setTextViewText(R.id.widget_title, (Objects.equals(json.getString("type"), "room") ? context.getString(R.string.room) + " " : "") + json.getString("label"));
                    views.setImageViewBitmap(R.id.widget_status, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                    views.removeAllViews(R.id.widget_container);
                    views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_list));
                    String title = "";
                    switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)){
                        case Calendar.MONDAY: title = context.getString(R.string.monday); break;
                        case Calendar.TUESDAY: title = context.getString(R.string.tuesday); break;
                        case Calendar.WEDNESDAY: title = context.getString(R.string.wednesday); break;
                        case Calendar.THURSDAY: title = context.getString(R.string.thursday); break;
                        case Calendar.FRIDAY: title = context.getString(R.string.friday); break;
                        case Calendar.SATURDAY: title = context.getString(R.string.saturday); break;
                        case Calendar.SUNDAY: title = context.getString(R.string.sunday); break;
                    }
                    int week = Static.getWeek(context) % 2;
                    views.setViewVisibility(R.id.widget_day_title, View.VISIBLE);
                    views.setTextViewText(R.id.widget_day_title, title + (week == 0 ? " (" + context.getString(R.string.tab_even) + ")" : (week == 1 ? " (" + context.getString(R.string.tab_odd) + ")" : "")));
                    views.setInt(R.id.widget_day_title, "setTextColor", colors.text);
                    new ScheduleLessonsAdditionalConverter(context, cache.getJSONObject("content"), new ScheduleLessonsAdditionalConverter.response() {
                        @Override
                        public void finish(final JSONObject content) {
                            ScheduleLessonsWidgetConfigureActivity.savePref(context, appWidgetId, "cache_converted", content.toString());
                            Intent adapter = new Intent(context, ScheduleLessonsWidgetService.class);
                            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                            adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
                            views.setRemoteAdapter(R.id.slw_day_schedule, adapter);
                            bindRefresh(context, appWidgetId, views);
                            bindOpen(context, appWidgetId, views);
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    appWidgetManager.updateAppWidget(appWidgetId, views);
                                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.slw_day_schedule);
                                }
                            });
                        }
                    }).run();
                } catch (Exception e) {
                    if (!(Objects.equals(e.getMessage(), "settings cannot be null") || Objects.equals(e.getMessage(), "cache cannot be null"))) {
                        Static.error(e);
                    }
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
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "failed | appWidgetId=" + appWidgetId + " | text=" + text);
                Colors colors = getColors(settings);
                final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_title, "setTextColor", colors.text);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.setImageViewBitmap(R.id.widget_status, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                views.setInt(R.id.slw_message_text, "setTextColor", colors.text);
                views.setTextViewText(R.id.slw_message_text, text);
                views.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_error_outline, colors.text));
                bindRefresh(context, appWidgetId, views);
                bindOpen(context, appWidgetId, views);
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                });
            }
        });
    }
    private static void needPreparations(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId) {
        Static.T.runThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "needPreparations | appWidgetId=" + appWidgetId);
                Colors colors = getColors();
                final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
                views.setInt(R.id.widget_content, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_header, "setBackgroundColor", colors.background);
                views.setInt(R.id.widget_title, "setTextColor", colors.text);
                views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
                views.setImageViewBitmap(R.id.widget_status, getBitmap(context, R.drawable.ic_widget_refresh, colors.text));
                views.removeAllViews(R.id.widget_container);
                views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
                views.setInt(R.id.slw_message_text, "setTextColor", colors.text);
                views.setImageViewBitmap(R.id.slw_message_icon, getBitmap(context, R.drawable.ic_widget_info_outline, colors.text));
                bindRefresh(context, appWidgetId, views);
                bindOpen(context, appWidgetId, views);
                Static.T.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                });
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

    private static void bindRefresh(final Context context, final int appWidgetId, final RemoteViews remoteViews) {
        Static.T.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent adapter = new Intent(context, ScheduleLessonsWidget.class);
                adapter.setAction(ACTION_WIDGET_UPDATE);
                adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
                remoteViews.setOnClickPendingIntent(R.id.widget_status_container, PendingIntent.getBroadcast(context, 0, adapter, 0));
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
                            if (settings != null) {
                                oIntent.putExtra("action_extra", new JSONObject(settings).getString("query"));
                            }
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
