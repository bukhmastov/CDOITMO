package com.bukhmastov.cdoitmo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

public class ScheduleLessonsWidget extends AppWidgetProvider {

    private static final String TAG = "ScheduleLessonsWidget";
    public static final String ACTION_WIDGET_UPDATE = "com.bukhmastov.cdoitmo.ACTION_WIDGET_UPDATE";
    public static final String ACTION_WIDGET_OPEN = "com.bukhmastov.cdoitmo.ACTION_WIDGET_OPEN";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean force) {
        //Log.d(TAG, "updateAppWidget | " + appWidgetId);
        try {
            JSONObject settings, cache;
            try {
                String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "settings");
                if (tmp == null) throw new NullPointerException("settings is null");
                settings = new JSONObject(tmp);
            } catch (Exception e) {
                settings = null;
            }
            try {
                String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "cache");
                if (tmp == null) throw new NullPointerException("cache is null");
                cache = new JSONObject(tmp);
            } catch (Exception e) {
                cache = null;
            }
            if (settings == null) {
                needPreparations(context, appWidgetManager, appWidgetId);
            } else if (cache == null || force) {
                update(context, appWidgetManager, appWidgetId, settings);
            } else {
                long timestamp = cache.getLong("timestamp");
                long shift = settings.getInt("updateTime") * 3600000;
                if (shift != 0 && timestamp + shift < Calendar.getInstance().getTimeInMillis()) {
                    update(context, appWidgetManager, appWidgetId, settings);
                } else {
                    display(context, appWidgetManager, appWidgetId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            //Log.d(TAG, "onDeleted " + appWidgetId);
            ScheduleLessonsWidgetConfigureActivity.deletePref(context, appWidgetId, "settings");
            ScheduleLessonsWidgetConfigureActivity.deletePref(context, appWidgetId, "cache");
        }
    }

    private static void update(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final JSONObject settings){
        //Log.d(TAG, "update");
        DeIfmoRestClient.init();
        ScheduleLessons scheduleLessons = new ScheduleLessons(context);
        scheduleLessons.setHandler(new ScheduleLessons.response(){
            @Override
            public void onProgress(int state) {
                progress(context, appWidgetManager, appWidgetId, settings);
            }
            @Override
            public void onFailure(int state) {
                switch (state) {
                    case DeIfmoRestClient.FAILED_OFFLINE:
                    case DeIfmoRestClient.FAILED_TRY_AGAIN:
                    case DeIfmoRestClient.FAILED_AUTH_TRY_AGAIN:
                    case ScheduleLessons.FAILED_LOAD:
                        failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
                        break;
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_REQUIRED:
                    case DeIfmoRestClient.FAILED_AUTH_CREDENTIALS_FAILED:
                        failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.widget_auth_failed));
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
                    e.printStackTrace();
                    failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
                }
            }
            @Override
            public void onNewHandle(RequestHandle requestHandle) {}
        });
        try {
            scheduleLessons.search(settings.getString("query"), 0);
        } catch (JSONException e) {
            e.printStackTrace();
            failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_load_schedule));
        }
    }

    private static void progress(Context context, AppWidgetManager appWidgetManager, int appWidgetId, JSONObject settings){
        //Log.d(TAG, "progress");
        int textColor, backGroundColor;
        boolean darkTheme;
        try {
            darkTheme = settings.getBoolean("darkTheme");
        } catch (JSONException e) {
            darkTheme = true;
        }
        if (darkTheme) {
            textColor = Color.parseColor("#FFFFFF");
            backGroundColor = Color.parseColor("#000000");
        } else {
            textColor = Color.parseColor("#000000");
            backGroundColor = Color.parseColor("#FFFFFF");
        }
        backGroundColor = Color.argb(150, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor));
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
        views.setInt(R.id.widget_content, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title_layout, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title, "setTextColor", textColor);
        views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
        //views.setImageViewResource(R.id.widget_status, darkTheme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
        views.removeAllViews(R.id.widget_container);
        views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_loading));
        views.setInt(R.id.slw_loading_text, "setTextColor", textColor);
        bindOpen(context, appWidgetId, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    private static void display(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        //Log.d(TAG, "display");
        JSONObject settings, cache;
        try {
            String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "settings");
            if (tmp == null) throw new NullPointerException("settings is null");
            settings = new JSONObject(tmp);
        } catch (Exception e) {
            settings = null;
        }
        try {
            String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "cache");
            if (tmp == null) throw new NullPointerException("cache is null");
            cache = new JSONObject(tmp);
        } catch (Exception e) {
            cache = null;
        }
        try {
            if (settings == null) throw new NullPointerException("settings cannot be null");
            if (cache == null) throw new NullPointerException("cache cannot be null");
            int textColor, backGroundColor;
            boolean darkTheme;
            try {
                darkTheme = settings.getBoolean("darkTheme");
            } catch (JSONException e) {
                darkTheme = true;
            }
            if (darkTheme) {
                textColor = Color.parseColor("#FFFFFF");
                backGroundColor = Color.parseColor("#000000");
            } else {
                textColor = Color.parseColor("#000000");
                backGroundColor = Color.parseColor("#FFFFFF");
            }
            backGroundColor = Color.argb(150, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor));
            JSONObject json = cache.getJSONObject("content");
            backGroundColor = Color.argb(150, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
            views.setInt(R.id.widget_content, "setBackgroundColor", backGroundColor);
            views.setInt(R.id.widget_title_layout, "setBackgroundColor", backGroundColor);
            views.setInt(R.id.widget_title, "setTextColor", textColor);
            views.setTextViewText(R.id.widget_title, (Objects.equals(json.getString("type"), "room") ? context.getString(R.string.room) + " " : "") + json.getString("scope"));
            views.setImageViewResource(R.id.widget_status, darkTheme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
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
            int week = getWeek(context);
            week = week % 2;
            views.setTextViewText(R.id.slw_day_title, title + (week == 0 ? " (" + context.getString(R.string.tab_even) + ")" : (week == 1 ? " (" + context.getString(R.string.tab_odd) + ")" : "")).toUpperCase());
            views.setInt(R.id.slw_day_title, "setTextColor", textColor);
            views.setInt(R.id.slw_day_title, "setBackgroundColor", Color.argb(80, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor)));
            Intent adapter = new Intent(context, ScheduleLessonsWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.slw_day_schedule, adapter);
            bindRefresh(context, appWidgetId, views);
            bindOpen(context, appWidgetId, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.slw_day_schedule);
        } catch (Exception e) {
            e.printStackTrace();
            if (Objects.equals(e.getMessage(), "settings cannot be null")) {
                needPreparations(context, appWidgetManager, appWidgetId);
            } else {
                failed(context, appWidgetManager, appWidgetId, settings, context.getString(R.string.failed_to_show_schedule));
            }
        }
    }
    private static void failed(Context context, AppWidgetManager appWidgetManager, int appWidgetId, JSONObject settings, String text){
        //Log.d(TAG, "failed | " + text);
        int textColor, backGroundColor;
        boolean darkTheme;
        try {
            darkTheme = settings.getBoolean("darkTheme");
        } catch (JSONException e) {
            darkTheme = true;
        }
        if (darkTheme) {
            textColor = Color.parseColor("#FFFFFF");
            backGroundColor = Color.parseColor("#000000");
        } else {
            textColor = Color.parseColor("#000000");
            backGroundColor = Color.parseColor("#FFFFFF");
        }
        backGroundColor = Color.argb(150, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor));
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
        views.setInt(R.id.widget_content, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title_layout, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title, "setTextColor", textColor);
        views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
        views.setImageViewResource(R.id.widget_status, darkTheme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
        views.removeAllViews(R.id.widget_container);
        views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
        views.setInt(R.id.slw_message_text, "setTextColor", textColor);
        views.setTextViewText(R.id.slw_message_text, text);
        views.setImageViewResource(R.id.slw_message_icon, darkTheme ? R.drawable.ic_widget_error_outline : R.drawable.ic_widget_info_outline_dark);
        bindRefresh(context, appWidgetId, views);
        bindOpen(context, appWidgetId, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    private static void needPreparations(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        int textColor, backGroundColor;
        boolean darkTheme = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_dark_theme", false);
        if (darkTheme) {
            textColor = Color.parseColor("#FFFFFF");
            backGroundColor = Color.parseColor("#000000");
        } else {
            textColor = Color.parseColor("#000000");
            backGroundColor = Color.parseColor("#FFFFFF");
        }
        backGroundColor = Color.argb(150, Color.red(backGroundColor), Color.green(backGroundColor), Color.blue(backGroundColor));
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget);
        views.setInt(R.id.widget_content, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title_layout, "setBackgroundColor", backGroundColor);
        views.setInt(R.id.widget_title, "setTextColor", textColor);
        views.setTextViewText(R.id.widget_title, context.getString(R.string.schedule_lessons));
        views.setImageViewResource(R.id.widget_status, darkTheme ? R.drawable.ic_widget_refresh : R.drawable.ic_widget_refresh_dark);
        views.removeAllViews(R.id.widget_container);
        views.addView(R.id.widget_container, new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_message));
        views.setInt(R.id.slw_message_text, "setTextColor", textColor);
        views.setImageViewResource(R.id.slw_message_icon, darkTheme ? R.drawable.ic_widget_info_outline : R.drawable.ic_widget_info_outline_dark);
        bindRefresh(context, appWidgetId, views);
        bindOpen(context, appWidgetId, views);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void bindRefresh(Context context, int appWidgetId, RemoteViews remoteViews){
        Intent adapter = new Intent(context, ScheduleLessonsWidget.class);
        adapter.setAction(ACTION_WIDGET_UPDATE);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setOnClickPendingIntent(R.id.widget_status, PendingIntent.getBroadcast(context, 0, adapter, 0));
    }
    private static void bindOpen(Context context, int appWidgetId, RemoteViews remoteViews){
        Intent adapter = new Intent(context, ScheduleLessonsWidget.class);
        adapter.setAction(ACTION_WIDGET_OPEN);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        adapter.setData(Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, adapter, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_content, pIntent);
    }
    private static int getWeek(Context context){
        try {
            String weekStr = Storage.get(context, "week");
            if(!Objects.equals(weekStr, "")){
                JSONObject jsonObject = new JSONObject(weekStr);
                int week = jsonObject.getInt("week");
                if (week >= 0){
                    Calendar past = Calendar.getInstance();
                    past.setTimeInMillis(jsonObject.getLong("timestamp"));
                    return week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                }
            }
        } catch (JSONException e) {
            Storage.delete(context, "week");
            return -1;
        }
        return -1;
    }

    public void onReceive(Context context, Intent intent) {
        int appWidgetId;
        switch(intent.getAction()){
            case ACTION_WIDGET_UPDATE:
                appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                updateAppWidget(context, appWidgetManager, appWidgetId, true);
                break;
            case ACTION_WIDGET_OPEN:
                appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                Intent oIntent = new Intent(context, SplashActivity.class);
                oIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                oIntent.putExtra("action", "schedule_lessons");
                try {
                    String settings = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "settings");
                    if (settings == null) throw new NullPointerException("settings is null");
                    oIntent.putExtra("action_extra", new JSONObject(settings).getString("query"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                context.startActivity(oIntent);
                break;
        }
        super.onReceive(context, intent);
    }

}

