package com.bukhmastov.cdoitmo.adapters;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsTabHostFragment;
import com.bukhmastov.cdoitmo.fragments.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.objects.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class ScheduleLessonsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "SLRVAdapter";

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DAY = 1;
    private static final int TYPE_LESSON_TOP = 2;
    private static final int TYPE_LESSON_REGULAR = 3;
    private static final int TYPE_LESSON_BOTTOM = 4;
    private static final int TYPE_LESSON_SINGLE = 5;
    private static final int TYPE_NOTIFICATION = 6;
    private static final int TYPE_UPDATE_TIME = 7;
    private static final int TYPE_NO_LESSONS = 8;
    private static final int TYPE_PICKER_HEADER = 9;
    private static final int TYPE_PICKER_ITEM = 10;
    private static final int TYPE_PICKER_NO_TEACHERS = 11;
    public static class Item {
        public int type;
        public JSONObject data;
        public Item (int type, JSONObject data) {
            this.type = type;
            this.data = data;
        }
    }
    public static class ItemLesson extends Item {
        public int weekday;
        public ItemLesson(int type, int weekday, JSONObject data) {
            super(type, data);
            this.weekday = weekday;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        protected final ViewGroup container;
        ViewHolder(ViewGroup container) {
            super(container);
            this.container = container;
        }
    }
    private final ConnectedActivity activity;
    private final int TYPE;
    private final JSONObject data;
    private final ArrayList<Item> dataset;
    private final SparseIntArray days_positions = new SparseIntArray();
    private final Static.StringCallback callback;
    private String reduced_lesson_mode = "compact";
    private String type = "";
    private String query = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1;

    public ScheduleLessonsRecyclerViewAdapter(final ConnectedActivity activity, int TYPE, JSONObject data, int weekday, final Static.StringCallback callback) {
        this.activity = activity;
        this.TYPE = TYPE;
        this.data = data;
        this.callback = callback;
        this.dataset = new ArrayList<>();
        try {
            reduced_lesson_mode = Storage.pref.get(activity, "pref_schedule_lessons_view_of_reduced_lesson", "compact");
            type = data.getString("type");
            query = data.getString("query");
            addItems(json2dataset(activity, data, weekday));
        } catch (Exception e) {
            Static.error(e);
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataset.get(position).type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        switch (viewType) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_DAY: layout = R.layout.layout_schedule_lessons_day; break;
            case TYPE_LESSON_TOP: layout = R.layout.layout_schedule_lessons_item_top; break;
            case TYPE_LESSON_REGULAR: layout = R.layout.layout_schedule_lessons_item_regular; break;
            case TYPE_LESSON_BOTTOM: layout = R.layout.layout_schedule_lessons_item_bottom; break;
            case TYPE_LESSON_SINGLE: layout = R.layout.layout_schedule_lessons_item_single; break;
            case TYPE_NOTIFICATION: layout = R.layout.layout_schedule_lessons_notification; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_LESSONS: layout = R.layout.nothing_to_display; break;
            case TYPE_PICKER_HEADER: layout = R.layout.layout_schedule_teacher_picker_header; break;
            case TYPE_PICKER_ITEM: layout = R.layout.layout_schedule_teacher_picker_item; break;
            case TYPE_PICKER_NO_TEACHERS: layout = R.layout.nothing_to_display; break;
            default: return null;
        }
        return new ViewHolder((ViewGroup) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = dataset.get(position);
        switch (item.type) {
            case TYPE_HEADER: {
                bindHeader(holder, item);
                break;
            }
            case TYPE_DAY: {
                bindDay(holder, item);
                break;
            }
            case TYPE_LESSON_TOP:
            case TYPE_LESSON_REGULAR:
            case TYPE_LESSON_BOTTOM:
            case TYPE_LESSON_SINGLE: {
                bindLesson(holder, (ItemLesson) item);
                break;
            }
            case TYPE_NOTIFICATION: {
                bindNotification(holder, item);
                break;
            }
            case TYPE_UPDATE_TIME: {
                bindUpdateTime(holder, item);
                break;
            }
            case TYPE_NO_LESSONS: {
                bindNoLessons(holder, item);
                break;
            }
            case TYPE_PICKER_HEADER: {
                bindPickerHeader(holder, item);
                break;
            }
            case TYPE_PICKER_ITEM: {
                bindPickerItem(holder, item);
                break;
            }
            case TYPE_PICKER_NO_TEACHERS: {
                bindPickerNoTeachers(holder, item);
                break;
            }
        }
    }

    private void bindHeader(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String title = getString(item.data, "title");
            final String week = getString(item.data, "week");
            ViewHolder viewHolder = (ViewHolder) holder;
            TextView schedule_lessons_header = viewHolder.container.findViewById(R.id.schedule_lessons_header);
            if (title != null && !title.isEmpty()) {
                schedule_lessons_header.setText(title);
            } else {
                ((ViewGroup) schedule_lessons_header.getParent()).removeView(schedule_lessons_header);
            }
            TextView schedule_lessons_week = viewHolder.container.findViewById(R.id.schedule_lessons_week);
            if (week != null && !week.isEmpty()) {
                schedule_lessons_week.setText(week);
            } else {
                ((ViewGroup) schedule_lessons_week.getParent()).removeView(schedule_lessons_week);
            }
            viewHolder.container.findViewById(R.id.schedule_lessons_menu).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    Static.T.runThread(new Runnable() {
                        @Override
                        public void run() {
                            final String cache_token = query == null ? null : query.toLowerCase();
                            final boolean cached = cache_token != null && !Storage.file.cache.get(activity, "schedule_lessons#lessons#" + cache_token, "").isEmpty();
                            Static.T.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        final PopupMenu popup = new PopupMenu(activity, view);
                                        final Menu menu = popup.getMenu();
                                        popup.getMenuInflater().inflate(R.menu.schedule_lessons, menu);
                                        menu.findItem(cached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            @Override
                                            public boolean onMenuItemClick(final MenuItem item) {
                                                Log.v(TAG, "menu | popup item | clicked | " + item.getTitle().toString());
                                                switch (item.getItemId()) {
                                                    case R.id.add_to_cache:
                                                    case R.id.remove_from_cache: {
                                                        try {
                                                            if (cache_token == null) {
                                                                Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                            } else {
                                                                if (Storage.file.cache.exists(activity, "schedule_lessons#lessons#" + cache_token)) {
                                                                    if (Storage.file.cache.delete(activity, "schedule_lessons#lessons#" + cache_token)) {
                                                                        Static.snackBar(activity, activity.getString(R.string.cache_false));
                                                                    } else {
                                                                        Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                                    }
                                                                } else {
                                                                    if (data == null) {
                                                                        Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                                    } else {
                                                                        if (Storage.file.cache.put(activity, "schedule_lessons#lessons#" + cache_token, data.toString())) {
                                                                            Static.snackBar(activity, activity.getString(R.string.cache_true));
                                                                        } else {
                                                                            Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            Static.error(e);
                                                            Static.snackBar(activity, activity.getString(R.string.cache_failed));
                                                        }
                                                        break;
                                                    }
                                                    case R.id.add_lesson: {
                                                        Static.T.runThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    if (!ScheduleLessons.createLesson(activity, query, data.getString("title"), type, Static.getWeekDay(), new JSONObject(), null)) {
                                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                                    }
                                                                } catch (Exception e) {
                                                                    Static.error(e);
                                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                                }
                                                            }
                                                        });
                                                        break;
                                                    }
                                                    case R.id.share_changes: {
                                                        Static.T.runThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    final Bundle extras = new Bundle();
                                                                    extras.putString("action", "share");
                                                                    extras.putString("query", query);
                                                                    extras.putString("type", type);
                                                                    extras.putString("title", data.getString("title"));
                                                                    Static.T.runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            activity.openActivityOrFragment(ScheduleLessonsShareFragment.class, extras);
                                                                        }
                                                                    });
                                                                } catch (Exception e) {
                                                                    Static.error(e);
                                                                }
                                                            }
                                                        });
                                                        break;
                                                    }
                                                    case R.id.remove_changes: {
                                                        Static.T.runThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                new AlertDialog.Builder(activity)
                                                                        .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                                                                        .setMessage(R.string.pref_schedule_lessons_clear_direct_additional_warning)
                                                                        .setIcon(R.drawable.ic_warning)
                                                                        .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                                                                            @Override
                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                Static.T.runThread(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {
                                                                                        Log.v(TAG, "menu | popup item | remove_changes | dialog accepted");
                                                                                        if (!ScheduleLessons.clearChanges(activity, query, new Static.SimpleCallback() {
                                                                                            @Override
                                                                                            public void onCall() {
                                                                                                ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                                                                            }
                                                                                        })) {
                                                                                            Static.snackBar(activity, activity.getString(R.string.no_changes));
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        })
                                                                        .setNegativeButton(R.string.cancel, null)
                                                                        .create().show();
                                                            }
                                                        });
                                                        break;
                                                    }
                                                    case R.id.open_settings: {
                                                        activity.openActivityOrFragment(ConnectedActivity.TYPE.stackable, SettingsScheduleLessonsFragment.class, null);
                                                        break;
                                                    }
                                                }
                                                return false;
                                            }
                                        });
                                        popup.show();
                                    } catch (Exception e) {
                                        Static.error(e);
                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                }
                            });
                        }
                    });
                }
            });
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindDay(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String text = getString(item.data, "text");
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.day_title)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindLesson(RecyclerView.ViewHolder holder, ItemLesson item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            final JSONObject lesson = item.data;
            final int weekday = item.weekday;
            final String cdoitmo_type = getString(lesson, "cdoitmo_type");
            final String type = getString(lesson, "type");
            final int week = getInt(lesson, "week");
            final boolean isReduced = "reduced".equals(cdoitmo_type);
            final boolean isCompact = isReduced && "compact".equals(reduced_lesson_mode);
            final boolean isSynthetic = "synthetic".equals(cdoitmo_type);
            final float alpha = isReduced ? 0.3F : 1F;
            // reduced and compact
            viewHolder.container.findViewById(R.id.time).setAlpha(alpha);
            viewHolder.container.findViewById(R.id.data).setAlpha(alpha);
            ((TextView) viewHolder.container.findViewById(R.id.lesson_title)).setMaxLines(isCompact ? 1 : 10);
            viewHolder.container.findViewById(R.id.lesson_time_icon).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            viewHolder.container.findViewById(R.id.lesson_time_end).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            viewHolder.container.findViewById(R.id.lesson_desc).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            viewHolder.container.findViewById(R.id.lesson_flags).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            viewHolder.container.findViewById(R.id.lesson_meta).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            // badges
            viewHolder.container.findViewById(R.id.lesson_reduced_icon).setVisibility(isReduced ? View.VISIBLE : View.GONE);
            viewHolder.container.findViewById(R.id.lesson_synthetic_icon).setVisibility(isSynthetic ? View.VISIBLE : View.GONE);
            viewHolder.container.findViewById(R.id.lesson_touch_icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Log.v(TAG, "lesson_touch_icon clicked");
                        final String group = getMenuTitle(lesson, "group");
                        final String teacher = getMenuTitle(lesson, "teacher");
                        final String teacher_id = getMenuTitle(lesson, "teacher", "teacher_id");
                        final String room = getMenuTitle(lesson, "room");
                        final String building = getMenuTitle(lesson, "building");
                        final PopupMenu popup = new PopupMenu(activity, view);
                        final Menu menu = popup.getMenu();
                        popup.getMenuInflater().inflate(R.menu.schedule_lessons_item, menu);
                        bindMenuItem(menu, R.id.open_group, group == null ? null : activity.getString(R.string.group) + " " + group);
                        bindMenuItem(menu, R.id.open_teacher, teacher == null || teacher_id == null ? null : teacher);
                        bindMenuItem(menu, R.id.open_room, room == null ? null : activity.getString(R.string.room) + " " + room);
                        bindMenuItem(menu, R.id.open_location, building);
                        bindMenuItem(menu, R.id.reduce_lesson, cdoitmo_type.equals("normal") ? activity.getString(R.string.reduce_lesson) : null);
                        bindMenuItem(menu, R.id.restore_lesson, cdoitmo_type.equals("reduced") ? activity.getString(R.string.restore_lesson) : null);
                        bindMenuItem(menu, R.id.delete_lesson, cdoitmo_type.equals("synthetic") ? activity.getString(R.string.delete_lesson) : null);
                        bindMenuItem(menu, R.id.edit_lesson, cdoitmo_type.equals("synthetic") ? activity.getString(R.string.edit_lesson) : null);
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                Log.v(TAG, "lesson_touch_icon | popup.MenuItem clicked | " + item.getTitle().toString());
                                switch (item.getItemId()) {
                                    case R.id.open_group: callback.onCall(group); break;
                                    case R.id.open_teacher: callback.onCall(teacher_id); break;
                                    case R.id.open_room: callback.onCall(room); break;
                                    case R.id.open_location:
                                        try {
                                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + building)));
                                        } catch (ActivityNotFoundException e) {
                                            Static.snackBar(activity, activity.getString(R.string.failed_to_start_geo_activity));
                                        }
                                        break;
                                    case R.id.reduce_lesson:
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!ScheduleLessons.reduceLesson(activity, query, weekday, lesson, new Static.SimpleCallback() {
                                                    @Override
                                                    public void onCall() {
                                                        ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                                    }
                                                })) {
                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            }
                                        });
                                        break;
                                    case R.id.restore_lesson:
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!ScheduleLessons.restoreLesson(activity, query, weekday, lesson, new Static.SimpleCallback() {
                                                    @Override
                                                    public void onCall() {
                                                        ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                                    }
                                                })) {
                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            }
                                        });
                                        break;
                                    case R.id.delete_lesson:
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!ScheduleLessons.deleteLesson(activity, query, weekday, lesson, new Static.SimpleCallback() {
                                                    @Override
                                                    public void onCall() {
                                                        ScheduleLessonsTabHostFragment.invalidateOnDemand();
                                                    }
                                                })) {
                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            }
                                        });
                                        break;
                                    case R.id.copy_lesson:
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (!ScheduleLessons.createLesson(activity, query, data.getString("title"), ScheduleLessonsRecyclerViewAdapter.this.type, weekday, lesson, null)) {
                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                    }
                                                } catch (Exception e) {
                                                    Static.error(e);
                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            }
                                        });
                                        break;
                                    case R.id.edit_lesson:
                                        Static.T.runThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (!ScheduleLessons.editLesson(activity, query, data.getString("title"), ScheduleLessonsRecyclerViewAdapter.this.type, weekday, lesson, null)) {
                                                        Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                    }
                                                } catch (Exception e) {
                                                    Static.error(e);
                                                    Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                                }
                                            }
                                        });
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
            // title and time
            ((TextView) viewHolder.container.findViewById(R.id.lesson_title)).setText(getString(lesson, "subject"));
            ((TextView) viewHolder.container.findViewById(R.id.lesson_time_start)).setText(getString(lesson, "timeStart"));
            ((TextView) viewHolder.container.findViewById(R.id.lesson_time_end)).setText(getString(lesson, "timeEnd"));
            // desc
            TextView lesson_desc = viewHolder.container.findViewById(R.id.lesson_desc);
            String desc = null;
            switch (this.type) {
                case "group": desc = getString(lesson, "teacher"); break;
                case "teacher": desc = getString(lesson, "group"); break;
                case "mine":
                case "room": {
                    String group = getString(lesson, "group");
                    String teacher = getString(lesson, "teacher");
                    if (group.isEmpty()) {
                        desc = teacher;
                    } else {
                        desc = group;
                        if (!teacher.isEmpty()) desc += " (" + teacher + ")";
                    }
                    break;
                }
            }
            desc = desc == null || desc.isEmpty() ? null : desc;
            if (desc == null) {
                lesson_desc.setVisibility(View.GONE);
            } else {
                lesson_desc.setText(desc);
            }
            // flags
            ViewGroup lesson_flags = viewHolder.container.findViewById(R.id.lesson_flags);
            lesson_flags.removeAllViews();
            if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Static.resolveColor(activity, R.attr.colorScheduleFlagTEXT);
            if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Static.resolveColor(activity, R.attr.colorScheduleFlagCommonBG);
            if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Static.resolveColor(activity, R.attr.colorScheduleFlagPracticeBG);
            if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLectureBG);
            if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Static.resolveColor(activity, R.attr.colorScheduleFlagLabBG);
            if (!type.isEmpty()) {
                switch (type) {
                    case "practice":
                        lesson_flags.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG));
                        break;
                    case "lecture":
                        lesson_flags.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG));
                        break;
                    case "lab":
                        lesson_flags.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG));
                        break;
                    default:
                        lesson_flags.addView(getFlag(type, colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
                        break;
                }
            }
            if (TYPE == 2 && (week == 0 || week == 1)) {
                lesson_flags.addView(getFlag(week == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
            }
            // meta
            TextView lesson_meta = viewHolder.container.findViewById(R.id.lesson_meta);
            String meta = null;
            switch (this.type) {
                case "mine":
                case "group":
                case "teacher": {
                    String room = getString(lesson, "room");
                    String building = getString(lesson, "building");
                    if (room.isEmpty()) {
                        meta = building;
                    } else {
                        meta = activity.getString(R.string.room_short) + " " + room;
                        if (!building.isEmpty()) {
                            meta += " (" + building + ")";
                        }
                    }
                    break;
                }
                case "room": {
                    meta = getString(lesson, "building");
                    break;
                }
            }
            meta = meta == null || meta.isEmpty() ? null : meta;
            if (meta == null) {
                lesson_meta.setVisibility(View.GONE);
            } else {
                lesson_meta.setText(meta);
            }
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNotification(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String text = getString(item.data, "text");
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.lessons_warning)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindUpdateTime(RecyclerView.ViewHolder holder, Item item) {
        try {
            final String text = getString(item.data, "text");
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.update_time)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindNoLessons(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(R.string.no_lessons);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerHeader(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.choose_teacher) + ":";
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.teachers_found) + ":";
            }
            ((TextView) viewHolder.container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerItem(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            final String pid = item.data.getString("pid");
            String teacher = item.data.getString("person");
            String post = item.data.getString("post");
            if (post != null && !post.isEmpty()) {
                teacher += " (" + post + ")";
            }
            ((TextView) viewHolder.container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            viewHolder.container.findViewById(R.id.teacher_picker_item).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (pid != null && !pid.isEmpty()) {
                        callback.onCall(pid);
                    }
                }
            });
        } catch (Exception e) {
            Static.error(e);
        }
    }
    private void bindPickerNoTeachers(RecyclerView.ViewHolder holder, Item item) {
        try {
            ViewHolder viewHolder = (ViewHolder) holder;
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.no_teachers);
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.no_teachers).toLowerCase();
            }
            ((TextView) viewHolder.container.findViewById(R.id.ntd_text)).setText(text);
        } catch (Exception e) {
            Static.error(e);
        }
    }

    public ArrayList<Item> json2dataset(ConnectedActivity activity, JSONObject json, int weekday) throws JSONException {
        final ArrayList<Item> dataset = new ArrayList<>();
        // check
        if (!ScheduleLessons.TYPE.equals(json.getString("schedule_type"))) {
            return dataset;
        }
        if (type.equals("teachers")) {
            // teacher picker mode
            final JSONArray schedule = json.getJSONArray("schedule");
            if (schedule.length() > 0) {
                dataset.add(new Item(TYPE_PICKER_HEADER, new JSONObject().put("query", query)));
                for (int i = 0; i < schedule.length(); i++) {
                    final JSONObject teacher = schedule.getJSONObject(i);
                    dataset.add(new Item(TYPE_PICKER_ITEM, teacher));
                }
            } else {
                dataset.add(new Item(TYPE_PICKER_NO_TEACHERS, new JSONObject().put("query", query)));
            }
        } else {
            // regular schedule mode
            int position = 0;
            // header
            dataset.add(new Item(TYPE_HEADER, new JSONObject()
                    .put("title", ScheduleLessons.getScheduleHeader(activity, json.getString("title"), json.getString("type")))
                    .put("week", ScheduleLessons.getScheduleWeek(activity, weekday))
            ));
            position++;
            // schedule
            final JSONArray schedule = json.getJSONArray("schedule");
            int lessons_count = 0;
            for (int i = 0; i < schedule.length(); i++) {
                final JSONObject day = schedule.getJSONObject(i);
                final int day_weekday = day.getInt("weekday");
                JSONArray lessons = day.getJSONArray("lessons");
                if (lessons.length() == 0) continue;
                final JSONArray lessonsFiltered = new JSONArray();
                for (int j = 0; j < lessons.length(); j++) {
                    final JSONObject lesson = lessons.getJSONObject(j);
                    if (!(TYPE == 2 || TYPE == lesson.getInt("week") || lesson.getInt("week") == 2)) continue;
                    if ("hidden".equals(reduced_lesson_mode) && "reduced".equals(lesson.getString("cdoitmo_type"))) continue;
                    lessonsFiltered.put(lesson);
                }
                lessons = lessonsFiltered;
                if (lessons.length() == 0) continue;
                // day title
                String day_title;
                switch (day_weekday) {
                    case 0: day_title = activity.getString(R.string.monday); break;
                    case 1: day_title = activity.getString(R.string.tuesday); break;
                    case 2: day_title = activity.getString(R.string.wednesday); break;
                    case 3: day_title = activity.getString(R.string.thursday); break;
                    case 4: day_title = activity.getString(R.string.friday); break;
                    case 5: day_title = activity.getString(R.string.saturday); break;
                    case 6: day_title = activity.getString(R.string.sunday); break;
                    default:
                        try {
                            if (day.has("type") && !day.isNull("type")) {
                                switch (day.getString("type")) {
                                    /*TODO implement when isu will be ready
                                    расписание из ису, когда есть расписания на определенный день
                                    case "date":
                                        if (day.has("title") && !day.isNull("title")) {
                                            day_title = day.getString("title");
                                        } else {
                                            day_title = activity.getString(R.string.unknown_day);
                                        }
                                        break;
                                    */
                                    default:
                                    case "unknown":
                                        day_title = activity.getString(R.string.unknown_day);
                                        break;
                                }
                            } else {
                                day_title = activity.getString(R.string.unknown_day);
                            }
                        } catch (Exception e) {
                            day_title = Static.GLITCH;
                        }
                        break;
                }
                dataset.add(new Item(TYPE_DAY, new JSONObject().put("text", day_title)));
                days_positions.append(day_weekday, position++);
                int day_lessons_count_total = lessons.length();
                for (int j = 0; j < day_lessons_count_total; j++) {
                    int type = TYPE_LESSON_REGULAR;
                    if (j == 0) {
                        if (day_lessons_count_total - 1 == 0) {
                            type = TYPE_LESSON_SINGLE;
                        } else {
                            type = TYPE_LESSON_TOP;
                        }
                    } else if (j == day_lessons_count_total - 1) {
                        type = TYPE_LESSON_BOTTOM;
                    }
                    dataset.add(new ItemLesson(type, day_weekday, lessons.getJSONObject(j)));
                    position++;
                    lessons_count++;
                }
            }
            if (lessons_count == 0) {
                dataset.add(new Item(TYPE_NO_LESSONS, null));
            } else {
                // notification
                Calendar calendar = Static.getCalendar();
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                if (month == Calendar.AUGUST && day > 21 || month == Calendar.SEPTEMBER && day < 21 || month == Calendar.JANUARY && day > 14 || month == Calendar.FEBRUARY && day < 14) {
                    dataset.add(new Item(TYPE_NOTIFICATION, new JSONObject().put("text", activity.getString(R.string.schedule_lessons_unstable_warning))));
                }
                // update time
                dataset.add(new Item(TYPE_UPDATE_TIME, new JSONObject().put("text", activity.getString(R.string.update_date) + " " + Static.getUpdateTime(activity, json.getLong("timestamp")))));
            }
        }
        // that's all
        return dataset;
    }
    public void addItem(Item item) {
        this.dataset.add(item);
        this.notifyItemInserted(this.dataset.size() - 1);
    }
    public void addItems(ArrayList<Item> dataset) {
        int itemStart = this.dataset.size() - 1;
        this.dataset.addAll(dataset);
        this.notifyItemRangeInserted(itemStart, dataset.size() - 1);
    }
    public void removeItem(int position) {
        this.dataset.remove(position);
        this.notifyItemRemoved(position);
        this.notifyItemRangeChanged(position, this.dataset.size() - 1);
    }

    public int getDayPosition(int weekday) {
        return days_positions.get(weekday, -1);
    }

    protected JSONObject getJsonObject(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONObject) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected JSONArray getJsonArray(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (object == null) {
                return null;
            } else {
                try {
                    return (JSONArray) object;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected String getString(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            Object object = json.get(key);
            if (json.isNull(key) || object == null) {
                return null;
            } else {
                try {
                    String value = (String) object;
                    return value.equals("null") ? null : value;
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    protected int getInt(JSONObject json, String key) throws JSONException {
        if (json.has(key)) {
            try {
                return json.getInt(key);
            } catch (Exception e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
    private FrameLayout getFlag(String text, int textColor, int backgroundColor) throws Exception {
        FrameLayout flagContainer = (FrameLayout) inflate(R.layout.layout_schedule_lessons_flag);
        TextView flag_content = flagContainer.findViewById(R.id.flag_content);
        flag_content.setText(text);
        flag_content.setBackgroundColor(backgroundColor);
        flag_content.setTextColor(textColor);
        return flagContainer;
    }
    private String getMenuTitle(JSONObject lesson, String type) throws JSONException {
        return getMenuTitle(lesson, type, type);
    }
    private String getMenuTitle(JSONObject lesson, String type1, String type2) throws JSONException {
        return !type.equals(type1) ? (lesson.has(type2) ? (lesson.getString(type2).isEmpty() ? null : lesson.getString(type2)) : null) : null;
    }
    private void bindMenuItem(Menu menu, int id, String text){
        if (text == null || text.isEmpty()) {
            menu.findItem(id).setVisible(false);
        } else {
            menu.findItem(id).setTitle(text);
        }
    }
    private View inflate(int layout) throws InflateException {
        return ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, null);
    }
}
