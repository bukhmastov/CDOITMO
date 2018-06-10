package com.bukhmastov.cdoitmo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.util.Static;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

class ScheduleLessonsWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;
    private ScheduleLessonsWidget.Colors colors;
    private String type = "group";
    private int week = -1;
    private JSONArray lessons;

    ScheduleLessonsWidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        this.lessons = new JSONArray();
        try {
            JSONObject settings = ScheduleLessonsWidget.Data.getJson(context, appWidgetId, "settings");
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
            JSONObject content = ScheduleLessonsWidget.Data.getJson(context, appWidgetId, "cache_converted");
            JSONObject settings = ScheduleLessonsWidget.Data.getJson(context, appWidgetId, "settings");
            if (content == null) throw new NullPointerException("content cannot be null");
            if (settings == null) throw new NullPointerException("settings cannot be null");
            try {
                if (!settings.has("shift")) {
                    settings.put("shift", 0);
                }
                if (!settings.has("shiftAutomatic")) {
                    settings.put("shiftAutomatic", 0);
                }
                final int shift = settings.getInt("shift") + settings.getInt("shiftAutomatic");
                final Calendar calendar = Static.getCalendar();
                if (shift != 0) {
                    calendar.add(Calendar.HOUR, shift * 24);
                }
                this.week = Static.getWeek(context, calendar) % 2;
                this.type = content.getString("type");
                final JSONArray schedule = content.getJSONArray("schedule");
                if (schedule != null) {
                    final int weekday = Static.getWeekDay(calendar);
                    this.lessons = ScheduleLessonsWidget.getLessonsForWeekday(schedule, week, weekday);
                } else {
                    this.lessons = new JSONArray();
                }
            } catch (Exception e) {
                Static.error(e);
                this.lessons = new JSONArray();
            }
        } catch (Exception e) {
            if (!("settings cannot be null".equals(e.getMessage()) || "content cannot be null".equals(e.getMessage()))) {
                Static.error(e);
            }
            this.lessons = new JSONArray();
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            if (position >= getCount()) {
                return null;
            }
            final JSONObject lesson = this.lessons.getJSONObject(position);
            if (lesson == null) throw new NullPointerException("lesson cannot be null");
            final RemoteViews layout = new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_item);
            layout.setInt(R.id.slw_item_time_start, "setTextColor", colors.text);
            layout.setInt(R.id.slw_item_time_end, "setTextColor", colors.text);
            layout.setTextViewText(R.id.slw_item_time_start, lesson.getString("timeStart"));
            layout.setTextViewText(R.id.slw_item_time_end, lesson.getString("timeEnd"));
            layout.setImageViewBitmap(R.id.slw_item_time_icon, ScheduleLessonsWidget.getBitmap(context, R.drawable.ic_widget_time, colors.text));
            String title = lesson.getString("subject");
            String type = lesson.getString("type").trim();
            switch (type) {
                case "practice": title += " (" + context.getString(R.string.practice) + ")"; break;
                case "lecture": title += " (" + context.getString(R.string.lecture) + ")"; break;
                case "lab": title += " (" + context.getString(R.string.lab) + ")"; break;
                case "iws": title += " (" + context.getString(R.string.iws) + ")"; break;
                default:
                    if (!type.isEmpty()) title += " (" + type + ")";
                    break;
            }
            if (this.week == -1) {
                switch (lesson.getInt("week")){
                    case 0: title += " (" + context.getString(R.string.tab_even) + ")"; break;
                    case 1: title += " (" + context.getString(R.string.tab_odd) + ")"; break;
                }
            }
            layout.setTextViewText(R.id.slw_item_title, title);
            layout.setInt(R.id.slw_item_title, "setTextColor", colors.text);
            String desc = "";
            switch (this.type) {
                case "group": desc = lesson.getString("teacher"); break;
                case "teacher": desc = lesson.getString("group"); break;
                case "mine":
                case "room": {
                    String group = lesson.getString("group");
                    String teacher = lesson.getString("teacher");
                    if (group.isEmpty()) {
                        desc = teacher;
                    } else {
                        desc = group;
                        if (!teacher.isEmpty()) desc += " (" + teacher + ")";
                    }
                    break;
                }
            }
            if (desc != null && !desc.isEmpty()) {
                layout.setTextViewText(R.id.slw_item_desc, desc);
                layout.setInt(R.id.slw_item_desc, "setTextColor", colors.text);
            } else {
                layout.setInt(R.id.slw_item_desc, "setHeight", 0);
            }
            String meta = "";
            switch (this.type) {
                case "mine":
                case "group":
                case "teacher": {
                    String room = lesson.getString("room");
                    String building = lesson.getString("building");
                    if (room.isEmpty()) {
                        meta = building;
                    } else {
                        meta = context.getString(R.string.room_short) + " " + room;
                        if (!building.isEmpty()) {
                            meta += " (" + building + ")";
                        }
                    }
                    break;
                }
                case "room": {
                    meta += lesson.getString("building");
                    break;
                }
            }
            if (meta != null && !meta.isEmpty()) {
                layout.setTextViewText(R.id.slw_item_meta, meta);
                layout.setInt(R.id.slw_item_meta, "setTextColor", colors.text);
            } else {
                layout.setInt(R.id.slw_item_meta, "setHeight", 0);
            }
            return layout;
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
        try {
            return new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_item_loading);
        } catch (Exception e) {
            return null;
        }
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
