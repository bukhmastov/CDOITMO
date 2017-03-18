package com.bukhmastov.cdoitmo.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

public class ScheduleLessonsWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    Context context;
    int appWidgetId;
    boolean darkTheme;
    int week;
    String type;
    JSONArray lessons;

    ScheduleLessonsWidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        try {
            String settings = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "settings");
            if (settings == null) throw new NullPointerException("settings cannot be null");
            this.darkTheme = new JSONObject(settings).getBoolean("darkTheme");
        } catch (Exception e) {
            this.darkTheme = true;
        }
    }

    @Override
    public void onCreate() {
        this.lessons = new JSONArray();
        updateWeek(context);
    }

    @Override
    public void onDataSetChanged() {
        JSONObject cache;
        try {
            String tmp = ScheduleLessonsWidgetConfigureActivity.getPref(context, appWidgetId, "cache");
            if (tmp == null) throw new NullPointerException("cache is null");
            cache = new JSONObject(tmp);
        } catch (Exception e) {
            cache = null;
        }
        try {
            int dayNumber = 0;
            switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)){
                case Calendar.MONDAY: dayNumber = 0; break;
                case Calendar.TUESDAY: dayNumber = 1; break;
                case Calendar.WEDNESDAY: dayNumber = 2; break;
                case Calendar.THURSDAY: dayNumber = 3; break;
                case Calendar.FRIDAY: dayNumber = 4; break;
                case Calendar.SATURDAY: dayNumber = 5; break;
                case Calendar.SUNDAY: dayNumber = 6; break;
            }
            if (cache == null) throw new NullPointerException("cache cannot be null");
            JSONObject content = cache.getJSONObject("content");
            if (content == null) throw new NullPointerException("content cannot be null");
            JSONArray schedule = content.getJSONArray("schedule");
            this.type = content.getString("type");
            if (schedule == null) throw new NullPointerException("schedule cannot be null");
            updateWeek(context);
            int week = this.week % 2;
            boolean found = false;
            for (int i = 0; i < schedule.length(); i++){
                JSONObject day = schedule.getJSONObject(i);
                if (day.getInt("index") == dayNumber){
                    JSONArray lessons = day.getJSONArray("lessons");
                    if (lessons == null) throw new NullPointerException("lessons cannot be null");
                    if (week == -1) {
                        this.lessons = lessons;
                    } else {
                        JSONArray tmp = new JSONArray();
                        for (int j = 0; j < lessons.length(); j++) {
                            JSONObject obj = lessons.getJSONObject(j);
                            if (obj.getInt("week") == 2 || obj.getInt("week") == week) tmp.put(obj);
                        }
                        this.lessons = tmp;
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                this.lessons = new JSONArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.lessons = new JSONArray();
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), darkTheme ? R.layout.schedule_lessons_widget_item : R.layout.schedule_lessons_widget_item);
            JSONObject lesson = this.lessons.getJSONObject(position);
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            int textColor = Color.parseColor(darkTheme ? "#FFFFFF" : "#000000");
            remoteViews.setTextViewText(R.id.slw_item_time_start, lesson.getString("timeStart"));
            remoteViews.setInt(R.id.slw_item_time_start, "setTextColor", textColor);
            remoteViews.setTextViewText(R.id.slw_item_time_end, lesson.getString("timeEnd"));
            remoteViews.setInt(R.id.slw_item_time_end, "setTextColor", textColor);
            remoteViews.setImageViewResource(R.id.slw_item_time_icon, darkTheme ? R.drawable.ic_widget_time : R.drawable.ic_widget_time_dark);
            String title = lesson.getString("subject");
            String type = lesson.getString("type");
            switch (type) {
                case "practice": title += " (" + context.getString(R.string.practice) + ")"; break;
                case "lecture": title += " (" + context.getString(R.string.lecture) + ")"; break;
                case "lab": title += " (" + context.getString(R.string.lab) + ")"; break;
                default:
                    if (!Objects.equals(type, "")) title += " (" + type + ")";
                    break;
            }
            int week = this.week % 2;
            if (week == -1) {
                switch (lesson.getInt("week")){
                    case 0: title += " (" + context.getString(R.string.tab_even) + ")"; break;
                    case 1: title += " (" + context.getString(R.string.tab_odd) + ")"; break;
                }
            }
            remoteViews.setTextViewText(R.id.slw_item_title, title);
            remoteViews.setInt(R.id.slw_item_title, "setTextColor", textColor);
            String desc = "";
            switch (this.type) {
                case "group":
                    desc = lesson.getString("teacher");
                    break;
                case "teacher":
                    desc = lesson.getString("group");
                    break;
                case "room":
                    String group = lesson.getString("group");
                    String teacher = lesson.getString("teacher");
                    if (Objects.equals(group, "")) {
                        desc = teacher;
                    } else {
                        desc = group;
                        if (!Objects.equals(teacher, "")) desc += " (" + teacher + ")";
                    }
                    break;
            }
            if (!Objects.equals(desc, "")) {
                remoteViews.setTextViewText(R.id.slw_item_desc, desc);
                remoteViews.setInt(R.id.slw_item_desc, "setTextColor", textColor);
            } else {
                remoteViews.setInt(R.id.slw_item_desc, "setHeight", 0);
            }
            String meta = "";
            switch (this.type) {
                case "group":
                case "teacher":
                    String room = lesson.getString("room");
                    String building = lesson.getString("building");
                    if (Objects.equals(room, "")) {
                        meta = building;
                    } else {
                        meta = context.getString(R.string.room_short) + " " + room;
                        if (!Objects.equals(building, ""))
                            meta += " (" + building + ")";
                    }
                    break;
                case "room":
                    meta += lesson.getString("building");
                    break;
            }
            if (!Objects.equals(meta, "")) {
                remoteViews.setTextViewText(R.id.slw_item_meta, meta);
                remoteViews.setInt(R.id.slw_item_meta, "setTextColor", textColor);
            } else {
                remoteViews.setInt(R.id.slw_item_meta, "setHeight", 0);
            }
            return remoteViews;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateWeek(Context context){
        try {
            String weekStr = Storage.file.perm.get(context, "user#week");
            if(!Objects.equals(weekStr, "")){
                JSONObject jsonObject = new JSONObject(weekStr);
                int week = jsonObject.getInt("week");
                if (week >= 0){
                    Calendar past = Calendar.getInstance();
                    past.setTimeInMillis(jsonObject.getLong("timestamp"));
                    this.week = week + (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) - past.get(Calendar.WEEK_OF_YEAR));
                } else {
                    this.week = -1;
                }
            } else {
                this.week = -1;
            }
        } catch (JSONException e) {
            Storage.file.perm.delete(context, "user#week");
            this.week = -1;
        }
    }

    @Override
    public int getCount() {
        return this.lessons.length();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDestroy() {}
}
