package com.bukhmastov.cdoitmo.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

class ScheduleLessonsWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;
    private ScheduleLessonsWidget.Colors colors;
    private String type;
    private JSONArray lessons;

    ScheduleLessonsWidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        this.lessons = new JSONArray();
        try {
            JSONObject settings = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "settings");
            if (settings == null) throw new NullPointerException("settings cannot be null");
            colors = ScheduleLessonsWidget.getColors(settings);
        } catch (Exception e) {
            colors = ScheduleLessonsWidget.getColors();
        }
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        try {
            JSONObject content = ScheduleLessonsWidgetConfigureActivity.getPrefJson(context, appWidgetId, "cache_converted");
            if (content == null) throw new NullPointerException("content cannot be null");
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
                JSONArray schedule = content.getJSONArray("schedule");
                type = content.getString("type");
                if (schedule == null) throw new NullPointerException("schedule cannot be null");
                int week = Static.getWeek(context) % 2;
                boolean found = false;
                for (int i = 0; i < schedule.length(); i++) {
                    JSONObject day = schedule.getJSONObject(i);
                    if (day.getInt("index") == dayNumber) {
                        JSONArray lessonsArr = day.getJSONArray("lessons");
                        if (lessonsArr == null) throw new NullPointerException("lessons cannot be null");
                        lessons = new JSONArray();
                        for (int j = 0; j < lessonsArr.length(); j++) {
                            JSONObject lessonObj = lessonsArr.getJSONObject(j);
                            if (week == -1 || (lessonObj.getInt("week") == 2 || lessonObj.getInt("week") == week)) {
                                if (!Objects.equals(lessonObj.getString("cdoitmo_type"), "reduced")) {
                                    lessons.put(lessonObj);
                                }
                            }
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    lessons = new JSONArray();
                }
            } catch (Exception e) {
                Static.error(e);
                lessons = new JSONArray();
            }
        } catch (Exception e) {
            Static.error(e);
            this.lessons = new JSONArray();
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            if (position >= this.lessons.length()) {
                return null;
            }
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.schedule_lessons_widget_item);
            JSONObject lesson = this.lessons.getJSONObject(position);
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            remoteViews.setTextViewText(R.id.slw_item_time_start, lesson.getString("timeStart"));
            remoteViews.setInt(R.id.slw_item_time_start, "setTextColor", colors.text);
            remoteViews.setTextViewText(R.id.slw_item_time_end, lesson.getString("timeEnd"));
            remoteViews.setInt(R.id.slw_item_time_end, "setTextColor", colors.text);
            remoteViews.setImageViewBitmap(R.id.slw_item_time_icon, ScheduleLessonsWidget.getBitmap(context, R.drawable.ic_widget_time, colors.text));
            String title = lesson.getString("subject");
            String type = lesson.getString("type").trim();
            switch (type) {
                case "practice": title += " (" + context.getString(R.string.practice) + ")"; break;
                case "lecture": title += " (" + context.getString(R.string.lecture) + ")"; break;
                case "lab": title += " (" + context.getString(R.string.lab) + ")"; break;
                default:
                    if (!type.isEmpty()) title += " (" + type + ")";
                    break;
            }
            int week = Static.getWeek(context) % 2;
            if (week == -1) {
                switch (lesson.getInt("week")){
                    case 0: title += " (" + context.getString(R.string.tab_even) + ")"; break;
                    case 1: title += " (" + context.getString(R.string.tab_odd) + ")"; break;
                }
            }
            remoteViews.setTextViewText(R.id.slw_item_title, title);
            remoteViews.setInt(R.id.slw_item_title, "setTextColor", colors.text);
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
                remoteViews.setInt(R.id.slw_item_desc, "setTextColor", colors.text);
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
                remoteViews.setInt(R.id.slw_item_meta, "setTextColor", colors.text);
            } else {
                remoteViews.setInt(R.id.slw_item_meta, "setHeight", 0);
            }
            return remoteViews;
        } catch (Exception e) {
            Static.error(e);
            return null;
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
