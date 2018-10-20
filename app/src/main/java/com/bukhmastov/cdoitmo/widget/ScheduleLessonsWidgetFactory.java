package com.bukhmastov.cdoitmo.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.widget.schedule.lessons.WSLSettings;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;

import javax.inject.Inject;

public class ScheduleLessonsWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;
    private ScheduleLessonsWidget.Colors colors;
    private String type = "group";
    private int week = -1;
    private ArrayList<SLesson> lessons;

    @Inject
    Log log;
    @Inject
    Time time;
    @Inject
    ScheduleLessonsWidgetStorage scheduleLessonsWidgetStorage;

    ScheduleLessonsWidgetFactory(Context context, Intent intent) {
        AppComponentProvider.getComponent().inject(this);
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        this.lessons = new ArrayList<>();
        try {
            WSLSettings settings = scheduleLessonsWidgetStorage.getSettings(appWidgetId);
            if (settings == null) {
                throw new NullPointerException("settings cannot be null");
            }
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
            this.lessons.clear();
            WSLSettings settings = scheduleLessonsWidgetStorage.getSettings(appWidgetId);
            SLessons schedule = scheduleLessonsWidgetStorage.getConverted(appWidgetId);
            if (settings == null || schedule == null || StringUtils.isBlank(schedule.getType())) {
                return;
            }
            int shift = settings.getShift() + settings.getShiftAutomatic();
            Calendar calendar = time.getCalendar();
            if (shift != 0) {
                calendar.add(Calendar.HOUR, shift * 24);
            }
            this.week = time.getWeek(context, calendar) % 2;
            this.type = schedule.getType();
            this.lessons.addAll(ScheduleLessonsWidget.getLessonsForWeekday(schedule, week, time.getWeekDay(calendar)));
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        try {
            if (position >= getCount()) {
                return null;
            }
            SLesson lesson = this.lessons.get(position);
            if (lesson == null) {
                throw new NullPointerException("lesson cannot be null");
            }
            RemoteViews layout = new RemoteViews(context.getPackageName(), R.layout.widget_schedule_lessons_item);
            layout.setInt(R.id.slw_item_time_start, "setTextColor", colors.text);
            layout.setInt(R.id.slw_item_time_end, "setTextColor", colors.text);
            layout.setTextViewText(R.id.slw_item_time_start, lesson.getTimeStart());
            layout.setTextViewText(R.id.slw_item_time_end, lesson.getTimeEnd());
            layout.setImageViewBitmap(R.id.slw_item_time_icon, ScheduleLessonsWidget.getBitmap(context, R.drawable.ic_widget_time, colors.text));
            String title = lesson.getSubject();
            String type = lesson.getType().trim();
            switch (type) {
                case "practice": title += " (" + context.getString(R.string.practice) + ")"; break;
                case "lecture": title += " (" + context.getString(R.string.lecture) + ")"; break;
                case "lab": title += " (" + context.getString(R.string.lab) + ")"; break;
                case "iws": title += " (" + context.getString(R.string.iws) + ")"; break;
                default:
                    if (StringUtils.isNotBlank(type)){
                        title += " (" + type + ")";
                    }
                    break;
            }
            if (this.week == -1) {
                switch (lesson.getParity()){
                    case 0: title += " (" + context.getString(R.string.tab_even) + ")"; break;
                    case 1: title += " (" + context.getString(R.string.tab_odd) + ")"; break;
                }
            }
            layout.setTextViewText(R.id.slw_item_title, title);
            layout.setInt(R.id.slw_item_title, "setTextColor", colors.text);
            String desc = "";
            switch (this.type) {
                case "group": desc = lesson.getTeacherName(); break;
                case "teacher": desc = lesson.getGroup(); break;
                case "mine":
                case "room": {
                    String group = lesson.getGroup();
                    String teacher = lesson.getTeacherName();
                    if (StringUtils.isBlank(group)) {
                        desc = teacher;
                    } else {
                        desc = group;
                        if (StringUtils.isNotBlank(teacher)){
                            desc += " (" + teacher + ")";
                        }
                    }
                    break;
                }
            }
            if (StringUtils.isNotBlank(desc)) {
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
                    String room = lesson.getRoom();
                    String building = lesson.getBuilding();
                    if (StringUtils.isBlank(room)) {
                        meta = building;
                    } else {
                        meta = context.getString(R.string.room_short) + " " + room;
                        if (StringUtils.isNotBlank(building)) {
                            meta += " (" + building + ")";
                        }
                    }
                    break;
                }
                case "room": {
                    meta += lesson.getBuilding();
                    break;
                }
            }
            if (StringUtils.isNotBlank(meta)) {
                layout.setTextViewText(R.id.slw_item_meta, meta);
                layout.setInt(R.id.slw_item_meta, "setTextColor", colors.text);
            } else {
                layout.setInt(R.id.slw_item_meta, "setHeight", 0);
            }
            return layout;
        } catch (Exception e) {
            log.exception(e);
            return null;
        }
    }

    @Override
    public int getCount() {
        return lessons.size();
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
