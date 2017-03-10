package com.bukhmastov.cdoitmo.builders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.entities.ScheduleMenuItem;
import com.bukhmastov.cdoitmo.utils.Static;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class ScheduleLessonsBuilder extends Thread {

    public interface response {
        void state(int state, View layout);
    }
    private response delegate = null;
    private Activity activity;
    private int type;
    private float destiny;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1;
    private int scheduleMenuItemIndex = 0;

    public static final int STATE_FAILED = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_DONE = 2;

    public ScheduleLessonsBuilder(Activity activity, int type, ScheduleLessonsBuilder.response delegate){
        this.activity = activity;
        this.delegate = delegate;
        this.type = type;
        this.destiny = activity.getResources().getDisplayMetrics().density;
    }
    public void run(){
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        try {
            delegate.state(STATE_LOADING, inflate(R.layout.state_loading_compact));
            Static.scheduleMenuItems.clear();
            scheduleMenuItemIndex = 0;
            JSONArray schedule = ScheduleLessonsFragment.schedule.getJSONArray("schedule");
            int daysCount = 0;
            for (int i = 0; i < schedule.length(); i++) {
                JSONObject day = schedule.getJSONObject(i);
                JSONArray lessons = day.getJSONArray("lessons");
                int lessonsCount = 0;
                LinearLayout dayLayout = (LinearLayout) inflate(R.layout.layout_schedule_lessons_day);
                ((TextView) dayLayout.findViewById(R.id.day_title)).setText(day.getString("title").toUpperCase());
                switch (day.getInt("index")) {
                    case 0: dayLayout.setId(R.id.monday); break;
                    case 1: dayLayout.setId(R.id.tuesday); break;
                    case 2: dayLayout.setId(R.id.wednesday); break;
                    case 3: dayLayout.setId(R.id.thursday); break;
                    case 4: dayLayout.setId(R.id.friday); break;
                    case 5: dayLayout.setId(R.id.saturday); break;
                    case 6: dayLayout.setId(R.id.sunday); break;
                }
                LinearLayout lessonsLayout = (LinearLayout) dayLayout.findViewById(R.id.day_schedule);
                for (int j = 0; j < lessons.length(); j++) {
                    final JSONObject lesson = lessons.getJSONObject(j);
                    if (!(type == 2 || type == lesson.getInt("week") || lesson.getInt("week") == 2)) continue;
                    final String group = !Objects.equals(ScheduleLessonsFragment.schedule.getString("type"), "group") ? (lesson.has("group") ? (Objects.equals(lesson.getString("group"), "") ? null : lesson.getString("group")) : null) : null;
                    final String teacher = !Objects.equals(ScheduleLessonsFragment.schedule.getString("type"), "teacher") ? (lesson.has("teacher") ? (Objects.equals(lesson.getString("teacher"), "") ? null : lesson.getString("teacher")) : null) : null;
                    final String teacher_id = !Objects.equals(ScheduleLessonsFragment.schedule.getString("type"), "teacher") ? (lesson.has("teacher_id") ? (Objects.equals(lesson.getString("teacher_id"), "") ? null : lesson.getString("teacher_id")) : null) : null;
                    final String room = !Objects.equals(ScheduleLessonsFragment.schedule.getString("type"), "room") ? (lesson.has("room") ? (Objects.equals(lesson.getString("room"), "") ? null : lesson.getString("room")) : null) : null;
                    lessonsCount++;
                    if (j != 0) {
                        View separator = new View(activity);
                        separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                        separator.setBackgroundColor(Static.colorSeparator);
                        lessonsLayout.addView(separator);
                    }
                    final LinearLayout lessonLayout = (LinearLayout) inflate(R.layout.layout_schedule_lessons_item);
                    ((TextView) lessonLayout.findViewById(R.id.lesson_time_start)).setText(lesson.getString("timeStart"));
                    ((TextView) lessonLayout.findViewById(R.id.lesson_time_end)).setText(lesson.getString("timeEnd"));
                    ((TextView) lessonLayout.findViewById(R.id.lesson_title)).setText(lesson.getString("subject"));
                    setDesc(lesson, (TextView) lessonLayout.findViewById(R.id.lesson_desc));
                    setFlags(lesson, (ViewGroup) lessonLayout.findViewById(R.id.lesson_flags));
                    setMeta(lesson, (TextView) lessonLayout.findViewById(R.id.lesson_meta));
                    if (group != null || teacher != null || room != null) {
                        lessonLayout.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                            @Override
                            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                                menu.setHeaderTitle(R.string.open_schedule);
                                if (group != null) {
                                    Static.scheduleMenuItems.put(scheduleMenuItemIndex, new ScheduleMenuItem(activity.getString(R.string.group) + " " + group, group));
                                    menu.add(0, scheduleMenuItemIndex, 0, Static.scheduleMenuItems.get(scheduleMenuItemIndex).label);
                                    scheduleMenuItemIndex++;
                                }
                                if (teacher != null) {
                                    Static.scheduleMenuItems.put(scheduleMenuItemIndex, new ScheduleMenuItem(teacher, teacher_id));
                                    menu.add(0, scheduleMenuItemIndex, 0, Static.scheduleMenuItems.get(scheduleMenuItemIndex).label);
                                    scheduleMenuItemIndex++;
                                }
                                if (room != null) {
                                    Static.scheduleMenuItems.put(scheduleMenuItemIndex, new ScheduleMenuItem(activity.getString(R.string.room) + " " + room, room));
                                    menu.add(0, scheduleMenuItemIndex, 0, Static.scheduleMenuItems.get(scheduleMenuItemIndex).label);
                                    scheduleMenuItemIndex++;
                                }
                            }
                        });
                    } else {
                        lessonLayout.findViewById(R.id.lesson_touch_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    }
                    lessonsLayout.addView(lessonLayout);
                }
                if (lessonsCount > 0) {
                    container.addView(dayLayout);
                    daysCount++;
                }
            }
            if (daysCount == 0) {
                container.addView(inflate(R.layout.layout_schedule_lessons_without_lessons));
            } else {
                TextView textView = new TextView(activity);
                textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setPadding((int) (16 * destiny), (int) (10 * destiny), (int) (16 * destiny), 0);
                textView.setTextColor(Static.textColorSecondary);
                textView.setTextSize(13);
                textView.setText(activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, ScheduleLessonsFragment.schedule.getLong("timestamp")));
                container.addView(textView);
            }
            delegate.state(STATE_DONE, container);
        } catch (Exception e){
            Static.error(e);
            delegate.state(STATE_FAILED, container);
        }
    }

    private View inflate(int layout) throws Exception {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
    private void setDesc(final JSONObject lesson, TextView textView) throws Exception {
        String desc = null;
        switch (ScheduleLessonsFragment.schedule.getString("type")) {
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
        desc = Objects.equals(desc, "") ? null : desc;
        if (desc == null) {
            textView.setHeight(0);
        } else {
            textView.setText(desc);
        }
    }
    private void setMeta(final JSONObject lesson, TextView textView) throws Exception {
        String meta = null;
        boolean isBuilding = false;
        switch (ScheduleLessonsFragment.schedule.getString("type")) {
            case "group":
            case "teacher":
                String room = lesson.getString("room");
                String building = lesson.getString("building");
                if (Objects.equals(room, "")) {
                    meta = building;
                    isBuilding = true;
                } else {
                    meta = activity.getString(R.string.room_short) + " " + room;
                    if (!Objects.equals(building, "")) {
                        meta += " (" + building + ")";
                        isBuilding = true;
                    }
                }
                break;
            case "room":
                meta = lesson.getString("building");
                isBuilding = true;
                break;
        }
        meta = Objects.equals(meta, "") ? null : meta;
        if (meta == null) {
            textView.setHeight(0);
        } else {
            textView.setText(meta);
            if (isBuilding) {
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + lesson.getString("building"))));
                        } catch (JSONException e) {
                            Static.error(e);
                        }
                    }
                });
            }
        }
    }
    private void setFlags(JSONObject lesson, ViewGroup viewGroup) throws Exception {
        String lType = lesson.getString("type");
        if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Static.resolveColor(activity, R.attr.colorScheduleFlagTEXT);
        if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Static.resolveColor(activity, R.attr.colorScheduleFlagCommonBG);
        if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Static.resolveColor(activity, R.attr.colorScheduleFlagPracticeBG);
        if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLectureBG);
        if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLabBG);
        if (!lType.isEmpty()) {
            switch (lType) {
                case "practice":
                    viewGroup.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG));
                    break;
                case "lecture":
                    viewGroup.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG));
                    break;
                case "lab":
                    viewGroup.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG));
                    break;
                default:
                    viewGroup.addView(getFlag(lType, colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
                    break;
            }
        }
        int week = lesson.getInt("week");
        if (type == 2 && (week == 0 || week == 1)) {
            viewGroup.addView(getFlag(week == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
        }
    }
    private FrameLayout getFlag(String text, int textColor, int backgroundColor) throws Exception {
        FrameLayout flagContainer = (FrameLayout) inflate(R.layout.layout_schedule_lessons_flag);
        TextView flag_content = (TextView) flagContainer.findViewById(R.id.flag_content);
        flag_content.setText(text);
        flag_content.setBackgroundColor(backgroundColor);
        flag_content.setTextColor(textColor);
        return flagContainer;
    }

}
