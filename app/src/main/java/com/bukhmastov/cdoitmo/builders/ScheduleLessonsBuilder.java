package com.bukhmastov.cdoitmo.builders;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.ScheduleLessons;
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
            final JSONArray schedule = ScheduleLessonsFragment.schedule.getJSONArray("schedule");
            final String cache_token = ScheduleLessonsFragment.schedule.getString("cache_token");
            int daysCount = 0;
            for (int i = 0; i < schedule.length(); i++) {
                JSONObject day = schedule.getJSONObject(i);
                JSONArray lessons = day.getJSONArray("lessons");
                int lessonsCount = 0;
                LinearLayout dayLayout = (LinearLayout) inflate(R.layout.layout_schedule_lessons_day);
                ((TextView) dayLayout.findViewById(R.id.day_title)).setText(day.getString("title").toUpperCase());
                final int index = day.getInt("index");
                dayLayout.findViewById(R.id.add_lesson).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ScheduleLessons.createLesson(activity, ScheduleLessonsFragment.schedule, index, type);
                        } catch (Exception e) {
                            Static.error(e);
                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }
                });
                switch (index) {
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
                    lessonsCount++;
                    if (j != 0) {
                        View separator = new View(activity);
                        separator.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * destiny)));
                        separator.setBackgroundColor(Static.colorSeparator);
                        lessonsLayout.addView(separator);
                    }
                    final String cdoitmo_type = lesson.getString("cdoitmo_type");
                    final LinearLayout lessonLayout = (LinearLayout) inflate(R.layout.layout_schedule_lessons_item);
                    if (Objects.equals(cdoitmo_type, "reduced")) {
                        float alpha = 0.3F;
                        lessonLayout.findViewById(R.id.time).setAlpha(alpha);
                        lessonLayout.findViewById(R.id.data).setAlpha(alpha);
                    } else {
                        lessonLayout.findViewById(R.id.lesson_reduced_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    }
                    if (!Objects.equals(cdoitmo_type, "synthetic")) {
                        lessonLayout.findViewById(R.id.lesson_synthetic_icon).setLayoutParams(new LinearLayout.LayoutParams(0, 0));
                    }
                    ((TextView) lessonLayout.findViewById(R.id.lesson_title)).setText(lesson.getString("subject"));
                    ((TextView) lessonLayout.findViewById(R.id.lesson_time_start)).setText(lesson.getString("timeStart"));
                    ((TextView) lessonLayout.findViewById(R.id.lesson_time_end)).setText(lesson.getString("timeEnd"));
                    setDesc(lesson, (TextView) lessonLayout.findViewById(R.id.lesson_desc));
                    setFlags(lesson, (ViewGroup) lessonLayout.findViewById(R.id.lesson_flags));
                    setMeta(lesson, (TextView) lessonLayout.findViewById(R.id.lesson_meta));
                    lessonLayout.findViewById(R.id.lesson_touch_icon).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            try {
                                final String group = getMenuTitle(lesson, "group", "group");
                                final String teacher = getMenuTitle(lesson, "teacher", "teacher");
                                final String teacher_id = getMenuTitle(lesson, "teacher", "teacher_id");
                                final String room = getMenuTitle(lesson, "room", "room");
                                final String building = getMenuTitle(lesson, "building", "building");
                                PopupMenu popup = new PopupMenu(activity, view);
                                Menu menu = popup.getMenu();
                                popup.getMenuInflater().inflate(R.menu.schedule_lessons_item, menu);
                                bindMenuItem(menu, R.id.open_group, activity.getString(R.string.group) + " " + group, group == null);
                                bindMenuItem(menu, R.id.open_teacher, teacher, teacher == null || teacher_id == null);
                                bindMenuItem(menu, R.id.open_room, activity.getString(R.string.room) + " " + room, room == null);
                                bindMenuItem(menu, R.id.open_location, building, building == null);
                                bindMenuItem(menu, R.id.reduce_lesson, activity.getString(R.string.reduce_lesson), !Objects.equals(cdoitmo_type, "normal"));
                                bindMenuItem(menu, R.id.restore_lesson, activity.getString(R.string.restore_lesson), !Objects.equals(cdoitmo_type, "reduced"));
                                bindMenuItem(menu, R.id.delete_lesson, activity.getString(R.string.delete_lesson), !Objects.equals(cdoitmo_type, "synthetic"));
                                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        switch (item.getItemId()) {
                                            case R.id.open_group:
                                                ScheduleLessonsFragment.searchAndClear(group);
                                                break;
                                            case R.id.open_teacher:
                                                ScheduleLessonsFragment.searchAndClear(teacher_id);
                                                break;
                                            case R.id.open_room:
                                                ScheduleLessonsFragment.searchAndClear(room);
                                                break;
                                            case R.id.open_location:
                                                try {
                                                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + building)));
                                                } catch (ActivityNotFoundException e) {
                                                    Static.toast(activity, activity.getString(R.string.failed_to_start_geo_activity));
                                                }
                                                break;
                                            case R.id.reduce_lesson:
                                                ScheduleLessons.reduceLesson(activity, cache_token, index, lesson);
                                                break;
                                            case R.id.restore_lesson:
                                                ScheduleLessons.restoreLesson(activity, cache_token, index, lesson);
                                                break;
                                            case R.id.delete_lesson:
                                                ScheduleLessons.deleteLesson(activity, cache_token, index, lesson);
                                                break;
                                        }
                                        return false;
                                    }
                                });
                                popup.show();
                            } catch (Exception e){
                                Static.error(e);
                            }
                        }
                    });
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
        switch (ScheduleLessonsFragment.schedule.getString("type")) {
            case "group":
            case "teacher":
                String room = lesson.getString("room");
                String building = lesson.getString("building");
                if (Objects.equals(room, "")) {
                    meta = building;
                } else {
                    meta = activity.getString(R.string.room_short) + " " + room;
                    if (!Objects.equals(building, "")) {
                        meta += " (" + building + ")";
                    }
                }
                break;
            case "room":
                meta = lesson.getString("building");
                break;
        }
        meta = Objects.equals(meta, "") ? null : meta;
        if (meta == null) {
            textView.setHeight(0);
        } else {
            textView.setText(meta);
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
    private String getMenuTitle(JSONObject lesson, String type1, String type2) throws JSONException {
        return !Objects.equals(ScheduleLessonsFragment.schedule.getString("type"), type1) ? (lesson.has(type2) ? (Objects.equals(lesson.getString(type2), "") ? null : lesson.getString(type2)) : null) : null;
    }
    private void bindMenuItem(Menu menu, int id, String text, boolean hide){
        if (hide) {
            menu.findItem(id).setVisible(false);
        } else {
            menu.findItem(id).setTitle(text);
        }
    }

}