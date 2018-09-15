package com.bukhmastov.cdoitmo.adapter.rva;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.util.SparseIntArray;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.event.events.ShareTextEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.interfaces.CallableString;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.JsonUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;

public class ScheduleLessonsRVA extends RVA {

    private static final String TAG = "ScheduleLessonsRVA";

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

    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    ScheduleLessons scheduleLessons;
    @Inject
    ScheduleLessonsTabHostFragmentPresenter tabHostPresenter;
    @Inject
    Storage storage;
    @Inject
    StoragePref storagePref;
    @Inject
    ScheduleLessonsHelper scheduleLessonsHelper;
    @Inject
    NotificationMessage notificationMessage;
    @Inject
    Time time;
    @Inject
    Lazy<FirebaseAnalyticsProvider> firebaseAnalyticsProvider;

    private final ConnectedActivity activity;
    private final int TYPE;
    private final JSONObject data;
    private final SparseIntArray days_positions = new SparseIntArray();
    private final CallableString callback;
    private String reduced_lesson_mode = "compact";
    private String type = "";
    private String query = null;
    private int colorScheduleFlagTEXT = -1, colorScheduleFlagCommonBG = -1, colorScheduleFlagPracticeBG = -1, colorScheduleFlagLectureBG = -1, colorScheduleFlagLabBG = -1, colorScheduleFlagIwsBG = -1;

    public ScheduleLessonsRVA(final ConnectedActivity activity, int TYPE, JSONObject data, int weekday, final CallableString callback) {
        super();
        AppComponentProvider.getComponent().inject(this);
        this.activity = activity;
        this.TYPE = TYPE;
        this.data = data;
        this.callback = callback;
        try {
            reduced_lesson_mode = storagePref.get(activity, "pref_schedule_lessons_view_of_reduced_lesson", "compact");
            type = data.getString("type");
            query = data.getString("query");
            addItems(json2dataset(activity, data, weekday));
        } catch (Exception e) {
            log.exception(e);
        }
    }

    @Override
    protected int onGetLayout(int type) throws NullPointerException {
        @LayoutRes int layout;
        switch (type) {
            case TYPE_HEADER: layout = R.layout.layout_schedule_both_header; break;
            case TYPE_DAY: layout = R.layout.layout_schedule_lessons_day; break;
            case TYPE_LESSON_TOP: layout = R.layout.layout_schedule_lessons_item_top; break;
            case TYPE_LESSON_REGULAR: layout = R.layout.layout_schedule_lessons_item_regular; break;
            case TYPE_LESSON_BOTTOM: layout = R.layout.layout_schedule_lessons_item_bottom; break;
            case TYPE_LESSON_SINGLE: layout = R.layout.layout_schedule_lessons_item_single; break;
            case TYPE_NOTIFICATION: layout = R.layout.layout_schedule_lessons_notification; break;
            case TYPE_UPDATE_TIME: layout = R.layout.layout_schedule_both_update_time; break;
            case TYPE_NO_LESSONS: layout = R.layout.state_nothing_to_display_compact; break;
            case TYPE_PICKER_HEADER: layout = R.layout.layout_schedule_teacher_picker_header; break;
            case TYPE_PICKER_ITEM: layout = R.layout.layout_schedule_teacher_picker_item; break;
            case TYPE_PICKER_NO_TEACHERS: layout = R.layout.state_nothing_to_display_compact; break;
            default: throw new NullPointerException("Invalid type provided");
        }
        return layout;
    }

    @Override
    protected void onBind(View container, RVA.Item item) {
        switch (item.type) {
            case TYPE_HEADER: {
                bindHeader(container, item);
                break;
            }
            case TYPE_DAY: {
                bindDay(container, item);
                break;
            }
            case TYPE_LESSON_TOP:
            case TYPE_LESSON_REGULAR:
            case TYPE_LESSON_BOTTOM:
            case TYPE_LESSON_SINGLE: {
                bindLesson(container, item);
                break;
            }
            case TYPE_NOTIFICATION: {
                bindNotification(container, item);
                break;
            }
            case TYPE_UPDATE_TIME: {
                bindUpdateTime(container, item);
                break;
            }
            case TYPE_NO_LESSONS: {
                bindNoLessons(container, item);
                break;
            }
            case TYPE_PICKER_HEADER: {
                bindPickerHeader(container, item);
                break;
            }
            case TYPE_PICKER_ITEM: {
                bindPickerItem(container, item);
                break;
            }
            case TYPE_PICKER_NO_TEACHERS: {
                bindPickerNoTeachers(container, item);
                break;
            }
        }
    }

    private void bindHeader(View container, Item item) {
        try {
            final String title = JsonUtils.getString(item.data, "title");
            final String week = JsonUtils.getString(item.data, "week");
            final TextView schedule_lessons_header = container.findViewById(R.id.schedule_lessons_header);
            final TextView schedule_lessons_week = container.findViewById(R.id.schedule_lessons_week);
            if (StringUtils.isNotBlank(title)) {
                schedule_lessons_header.setText(title);
            } else {
                ((ViewGroup) schedule_lessons_header.getParent()).removeView(schedule_lessons_header);
            }
            if (StringUtils.isNotBlank(week)) {
                schedule_lessons_week.setText(week);
            } else {
                ((ViewGroup) schedule_lessons_week.getParent()).removeView(schedule_lessons_week);
            }
            container.findViewById(R.id.schedule_lessons_menu).setOnClickListener(this::showOptionsMenu);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindDay(View container, Item item) {
        try {
            final String text = JsonUtils.getString(item.data, "text");
            ((TextView) container.findViewById(R.id.day_title)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindLesson(View container, Item item) {
        try {
            final JSONObject lesson = item.data;
            final int weekday = (int) item.extras.get("weekday");
            final String cdoitmo_type = JsonUtils.getString(lesson, "cdoitmo_type");
            final String type = JsonUtils.getString(lesson, "type");
            final int week = JsonUtils.getInt(lesson, "week", -1);
            final boolean isReduced = "reduced".equals(cdoitmo_type);
            final boolean isCompact = isReduced && "compact".equals(reduced_lesson_mode);
            final boolean isSynthetic = "synthetic".equals(cdoitmo_type);
            final float alpha = isReduced ? 0.3F : 1F;
            // reduced and compact
            container.findViewById(R.id.time).setAlpha(alpha);
            container.findViewById(R.id.data).setAlpha(alpha);
            ((TextView) container.findViewById(R.id.lesson_title)).setMaxLines(isCompact ? 1 : 10);
            container.findViewById(R.id.lesson_time_icon).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_time_end).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_desc).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_flags).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            container.findViewById(R.id.lesson_meta).setVisibility(isCompact ? View.GONE : View.VISIBLE);
            // badges
            container.findViewById(R.id.lesson_reduced_icon).setVisibility(isReduced ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.lesson_synthetic_icon).setVisibility(isSynthetic ? View.VISIBLE : View.GONE);
            container.findViewById(R.id.lesson_touch_icon).setOnClickListener(view -> {
                try {
                    log.v(TAG, "lesson_touch_icon clicked");
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
                    popup.setOnMenuItemClickListener(item1 -> {
                        log.v(TAG, "lesson_touch_icon | popup.MenuItem clicked | " + item1.getTitle().toString());
                        switch (item1.getItemId()) {
                            case R.id.open_group: callback.call(group); break;
                            case R.id.open_teacher: callback.call(teacher_id); break;
                            case R.id.open_room: callback.call(room); break;
                            case R.id.open_location:
                                eventBus.fire(new OpenIntentEvent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Санкт-Петербург, " + building))).withIdentity(ScheduleLessonsRVA.class.getName()));
                                break;
                            case R.id.reduce_lesson:
                                thread.run(() -> {
                                    if (!scheduleLessonsHelper.reduceLesson(activity, storage, query, weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                });
                                break;
                            case R.id.restore_lesson:
                                thread.run(() -> {
                                    if (!scheduleLessonsHelper.restoreLesson(activity, storage, query, weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                });
                                break;
                            case R.id.delete_lesson:
                                thread.run(() -> {
                                    if (!scheduleLessonsHelper.deleteLesson(activity, storage, query, weekday, lesson, () -> tabHostPresenter.invalidateOnDemand())) {
                                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                });
                                break;
                            case R.id.copy_lesson:
                                thread.run(() -> {
                                    try {
                                        if (!scheduleLessonsHelper.createLesson(activity, query, data.getString("title"), ScheduleLessonsRVA.this.type, weekday, lesson, null)) {
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    } catch (Exception e) {
                                        log.exception(e);
                                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                });
                                break;
                            case R.id.edit_lesson:
                                thread.run(() -> {
                                    try {
                                        if (!scheduleLessonsHelper.editLesson(activity, query, data.getString("title"), ScheduleLessonsRVA.this.type, weekday, lesson, null)) {
                                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                        }
                                    } catch (Exception e) {
                                        log.exception(e);
                                        notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                                    }
                                });
                                break;
                        }
                        return false;
                    });
                    popup.show();
                } catch (Exception e){
                    log.exception(e);
                }
            });
            // title and time
            ((TextView) container.findViewById(R.id.lesson_title)).setText(JsonUtils.getString(lesson, "subject"));
            ((TextView) container.findViewById(R.id.lesson_time_start)).setText(JsonUtils.getString(lesson, "timeStart"));
            ((TextView) container.findViewById(R.id.lesson_time_end)).setText(JsonUtils.getString(lesson, "timeEnd"));
            // desc
            TextView lesson_desc = container.findViewById(R.id.lesson_desc);
            String desc = getLessonDesc(lesson, this.type);
            if (StringUtils.isBlank(desc)) {
                lesson_desc.setVisibility(View.GONE);
            } else {
                lesson_desc.setText(desc);
            }
            // flags
            ViewGroup lesson_flags = container.findViewById(R.id.lesson_flags);
            lesson_flags.removeAllViews();
            if (colorScheduleFlagTEXT == -1) colorScheduleFlagTEXT = Color.resolve(activity, R.attr.colorScheduleFlagTEXT);
            if (colorScheduleFlagCommonBG == -1) colorScheduleFlagCommonBG = Color.resolve(activity, R.attr.colorScheduleFlagCommonBG);
            if (colorScheduleFlagPracticeBG == -1) colorScheduleFlagPracticeBG = Color.resolve(activity, R.attr.colorScheduleFlagPracticeBG);
            if (colorScheduleFlagLectureBG == -1) colorScheduleFlagLectureBG = Color.resolve(activity, R.attr.colorScheduleFlagLectureBG);
            if (colorScheduleFlagLabBG == -1) colorScheduleFlagLabBG = Color.resolve(activity, R.attr.colorScheduleFlagLabBG);
            if (colorScheduleFlagIwsBG == -1) colorScheduleFlagIwsBG = Color.resolve(activity, R.attr.colorScheduleFlagIwsBG);
            if (!type.isEmpty()) {
                switch (type) {
                    case "practice": lesson_flags.addView(getFlag(activity.getString(R.string.practice), colorScheduleFlagTEXT, colorScheduleFlagPracticeBG)); break;
                    case "lecture": lesson_flags.addView(getFlag(activity.getString(R.string.lecture), colorScheduleFlagTEXT, colorScheduleFlagLectureBG)); break;
                    case "lab": lesson_flags.addView(getFlag(activity.getString(R.string.lab), colorScheduleFlagTEXT, colorScheduleFlagLabBG)); break;
                    case "iws": lesson_flags.addView(getFlag(activity.getString(R.string.iws), colorScheduleFlagTEXT, colorScheduleFlagIwsBG)); break;
                    default: lesson_flags.addView(getFlag(type, colorScheduleFlagTEXT, colorScheduleFlagCommonBG)); break;
                }
            }
            if (TYPE == 2 && (week == 0 || week == 1)) {
                lesson_flags.addView(getFlag(week == 0 ? activity.getString(R.string.tab_even) : activity.getString(R.string.tab_odd), colorScheduleFlagTEXT, colorScheduleFlagCommonBG));
            }
            // meta
            TextView lesson_meta = container.findViewById(R.id.lesson_meta);
            String meta = getLessonMeta(lesson, this.type);
            if (StringUtils.isBlank(meta)) {
                lesson_meta.setVisibility(View.GONE);
            } else {
                lesson_meta.setText(meta);
            }
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNotification(View container, Item item) {
        try {
            final String text = JsonUtils.getString(item.data, "text");
            ((TextView) container.findViewById(R.id.lessons_warning)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindUpdateTime(View container, Item item) {
        try {
            final String text = JsonUtils.getString(item.data, "text");
            ((TextView) container.findViewById(R.id.update_time)).setText(text != null && !text.isEmpty() ? text : Static.GLITCH);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindNoLessons(View container, Item item) {
        try {
            ((TextView) container.findViewById(R.id.ntd_text)).setText(R.string.no_lessons);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPickerHeader(View container, Item item) {
        try {
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.choose_teacher) + ":";
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.teachers_found) + ":";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_header)).setText(text);
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPickerItem(View container, Item item) {
        try {
            final String pid = item.data.getString("pid");
            String teacher = item.data.getString("person");
            String post = item.data.getString("post");
            if (post != null && !post.isEmpty()) {
                teacher += " (" + post + ")";
            }
            ((TextView) container.findViewById(R.id.teacher_picker_title)).setText(teacher);
            container.findViewById(R.id.teacher_picker_item).setOnClickListener(view -> {
                if (pid != null && !pid.isEmpty()) {
                    callback.call(pid);
                }
            });
        } catch (Exception e) {
            log.exception(e);
        }
    }
    private void bindPickerNoTeachers(View container, Item item) {
        try {
            String query = item.data.getString("query");
            String text;
            if (query == null || query.isEmpty()) {
                text = activity.getString(R.string.no_teachers);
            } else {
                text = activity.getString(R.string.on_search_for) + " \"" + query + "\" " + activity.getString(R.string.no_teachers).toLowerCase();
            }
            ((TextView) container.findViewById(R.id.ntd_text)).setText(text);
        } catch (Exception e) {
            log.exception(e);
        }
    }

    private void showOptionsMenu(View anchor) {
        thread.run(() -> {
            final String cacheToken = query == null ? null : query.toLowerCase();
            final boolean isCached = cacheToken != null && !storage.get(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, "").isEmpty();
            thread.runOnUI(() -> {
                try {
                    final PopupMenu popup = new PopupMenu(activity, anchor);
                    final Menu menu = popup.getMenu();
                    popup.getMenuInflater().inflate(R.menu.schedule_lessons, menu);
                    menu.findItem(isCached ? R.id.add_to_cache : R.id.remove_from_cache).setVisible(false);
                    popup.setOnMenuItemClickListener(menuItem -> {
                        optionsMenuClicked(menuItem, cacheToken);
                        return false;
                    });
                    popup.show();
                } catch (Exception e) {
                    log.exception(e);
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            });
        });
    }
    private void optionsMenuClicked(MenuItem item, String cacheToken) {
        log.v(TAG, "menu | popup item | clicked | " + item.getTitle().toString());
        switch (item.getItemId()) {
            case R.id.add_to_cache:
            case R.id.remove_from_cache: {
                toggleCache(cacheToken);
                break;
            }
            case R.id.add_lesson: {
                addLesson();
                break;
            }
            case R.id.add_military_day: {
                addMilitaryDay();
                break;
            }
            case R.id.share_schedule: {
                shareSchedule();
                break;
            }
            case R.id.share_changes: {
                shareChanges();
                break;
            }
            case R.id.remove_changes: {
                clearChanges();
                break;
            }
            case R.id.open_settings: {
                activity.openActivityOrFragment(ConnectedActivity.TYPE.STACKABLE, SettingsScheduleLessonsFragment.class, null);
                break;
            }
        }
    }
    private void toggleCache(String cacheToken) {
        thread.run(() -> {
            try {
                if (cacheToken == null) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                    return;
                }
                if (storage.exists(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
                    if (storage.delete(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken)) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_false));
                    } else {
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                    }
                } else {
                    if (data == null) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                        return;
                    }
                    if (storage.put(activity, Storage.CACHE, Storage.GLOBAL, "schedule_lessons#lessons#" + cacheToken, data.toString())) {
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_true));
                    } else {
                        notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
                    }
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.cache_failed));
            }
        });
    }
    private void addLesson() {
        thread.run(() -> {
            try {
                if (!scheduleLessonsHelper.createLesson(activity, query, data.getString("title"), type, time.getWeekDay(), new JSONObject(), null)) {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                }
            } catch (Exception e) {
                log.exception(e);
                notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
            }
        });
    }
    private void addMilitaryDay() {
        thread.runOnUI(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            final List<String> days = new ArrayList<>(Arrays.asList(activity.getString(R.string.monday), activity.getString(R.string.tuesday), activity.getString(R.string.wednesday), activity.getString(R.string.thursday), activity.getString(R.string.friday), activity.getString(R.string.saturday), activity.getString(R.string.sunday)));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, R.layout.spinner_center);
            arrayAdapter.addAll(days);
            new AlertDialog.Builder(activity)
                    .setAdapter(arrayAdapter, (dialogInterface, position) -> thread.run(() -> {
                        try {
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "Утренний осмотр, строевая подготовка").put("type", "Военка").put("week", 2).put("timeStart", "9:05").put("timeEnd", "9:20").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "1 пара").put("type", "Военка").put("week", 2).put("timeStart", "9:30").put("timeEnd", "10:50").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "2 пара").put("type", "Военка").put("week", 2).put("timeStart", "11:00").put("timeEnd", "12:20").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "3 пара").put("type", "Военка").put("week", 2).put("timeStart", "12:30").put("timeEnd", "13:50").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "4 пара").put("type", "Военка").put("week", 2).put("timeStart", "14:50").put("timeEnd", "16:10").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "Строевая подготовка").put("type", "Военка").put("week", 2).put("timeStart", "16:20").put("timeEnd", "16:35").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            scheduleLessonsHelper.createLesson(activity, storage, query, position, new JSONObject().put("subject", "Кураторский час").put("type", "Военка").put("week", 2).put("timeStart", "16:45").put("timeEnd", "17:30").put("group", "").put("teacher", "").put("teacher_id", "").put("room", "").put("building", "").put("cdoitmo_type", "synthetic"), null);
                            tabHostPresenter.invalidateOnDemand();
                        } catch (Exception e) {
                            log.exception(e);
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }))
                    .setNegativeButton(R.string.do_cancel, null)
                    .create().show();
        });
    }
    private void shareSchedule() {
        thread.run(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(scheduleLessons.getScheduleHeader(activity, JsonUtils.getString(data, "title"), JsonUtils.getString(data, "type")));
            sb.append("\n");
            JSONArray schedule = JsonUtils.getJsonArray(data, "schedule");
            if (schedule == null || schedule.length() == 0) {
                sb.append(activity.getString(R.string.no_lessons));
            } else {
                String type = JsonUtils.getString(data, "type", "");
                if (shareScheduleIsScheduleHasEvenOddWeekLessons(schedule)) {
                    shareScheduleAppendLessonsForWeek(sb, schedule, type, 0);
                    shareScheduleAppendLessonsForWeek(sb, schedule, type, 1);
                } else {
                    shareScheduleAppendLessonsForWeek(sb, schedule, type, 2);
                }
            }
            eventBus.fire(new ShareTextEvent(sb.toString(), "schedule_lessons_plain"));
        });
    }
    private boolean shareScheduleIsScheduleHasEvenOddWeekLessons(JSONArray schedule) {
        for (int i = 0; i < schedule.length(); i++) {
            JSONObject day = JsonUtils.getJsonObject(schedule, i);
            if (day == null) {
                continue;
            }
            JSONArray lessons = JsonUtils.getJsonArray(day, "lessons");
            if (lessons == null) {
                continue;
            }
            for (int j = 0; j < lessons.length(); j++) {
                JSONObject lesson = JsonUtils.getJsonObject(lessons, j);
                if (lesson != null && JsonUtils.getInt(lesson, "week", -1) != 2) {
                    return true;
                }
            }
        }
        return false;
    }
    private void shareScheduleAppendLessonsForWeek(StringBuilder sb, JSONArray schedule, String scheduleType, int week) {
        if (week == 0 || week == 1) {
            sb.append(activity.getString(week == 0 ? R.string.week_even : R.string.week_odd).toUpperCase());
            sb.append("\n");
        }
        int lessonsThisWeek = 0;
        for (int i = 0; i < schedule.length(); i++) {
            JSONObject day = JsonUtils.getJsonObject(schedule, i);
            if (day == null) {
                continue;
            }
            JSONArray lessons = JsonUtils.getJsonArray(day, "lessons");
            if (lessons == null || lessons.length() == 0) {
                continue;
            }
            lessons = filterLessons(lessons, week, true);
            if (lessons == null || lessons.length() == 0) {
                continue;
            }
            sb.append(getDayTitle(day)).append("\n");
            for (int j = 0; j < lessons.length(); j++) {
                JSONObject lesson = JsonUtils.getJsonObject(lessons, j);
                if (lesson == null) {
                    continue;
                }
                lessonsThisWeek++;
                String subject = JsonUtils.getString(lesson, "subject", Static.GLITCH);
                String desc = getLessonDesc(lesson, scheduleType);
                String meta = getLessonMeta(lesson, scheduleType);
                String lessonType = JsonUtils.getString(lesson, "type", "");
                switch (lessonType) {
                    case "practice": lessonType = activity.getString(R.string.practice); break;
                    case "lecture": lessonType = activity.getString(R.string.lecture); break;
                    case "lab": lessonType = activity.getString(R.string.lab); break;
                    case "iws": lessonType = activity.getString(R.string.iws); break;
                }
                sb.append(JsonUtils.getString(lesson, "timeStart", "∞"));
                sb.append("-");
                sb.append(JsonUtils.getString(lesson, "timeEnd", "∞"));
                sb.append(" ");
                sb.append(subject);
                sb.append(". ");
                if (StringUtils.isNotBlank(lessonType)) {
                    sb.append(lessonType).append(". ");
                }
                if (StringUtils.isNotBlank(desc)) {
                    sb.append(desc).append(". ");
                }
                if (StringUtils.isNotBlank(meta)) {
                    sb.append(meta).append(". ");
                }
                sb.append("\n");
            }
        }
        if (lessonsThisWeek == 0) {
            sb.append(activity.getString(R.string.no_lessons)).append("\n");
        }
    }
    private void shareChanges() {
        thread.run(() -> {
            try {
                final Bundle extras = new Bundle();
                extras.putString("action", "share");
                extras.putString("query", query);
                extras.putString("type", type);
                extras.putString("title", data.getString("title"));
                thread.runOnUI(() -> activity.openActivityOrFragment(ScheduleLessonsShareFragment.class, extras));
            } catch (Exception e) {
                log.exception(e);
            }
        });
    }
    private void clearChanges() {
        thread.run(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.pref_schedule_lessons_clear_additional_title)
                    .setMessage(R.string.pref_schedule_lessons_clear_direct_additional_warning)
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.proceed, (dialog, which) -> thread.run(() -> {
                        log.v(TAG, "menu | popup item | remove_changes | dialog accepted");
                        if (!scheduleLessonsHelper.clearChanges(activity, storage, query, () -> tabHostPresenter.invalidateOnDemand())) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.no_changes));
                        }
                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .create().show();
        });
    }

    private ArrayList<Item> json2dataset(ConnectedActivity activity, JSONObject json, int weekday) throws JSONException {
        final ArrayList<Item> dataset = new ArrayList<>();
        // check
        if (!ScheduleLessons.TYPE.equals(json.getString("schedule_type"))) {
            return dataset;
        }
        if (type.equals("teachers")) {
            // teacher picker mode
            final JSONArray schedule = json.getJSONArray("schedule");
            if (schedule.length() > 0) {
                dataset.add(getNewItem(TYPE_PICKER_HEADER, new JSONObject().put("query", query)));
                for (int i = 0; i < schedule.length(); i++) {
                    final JSONObject teacher = schedule.getJSONObject(i);
                    dataset.add(getNewItem(TYPE_PICKER_ITEM, teacher));
                }
            } else {
                dataset.add(getNewItem(TYPE_PICKER_NO_TEACHERS, new JSONObject().put("query", query)));
            }
        } else {
            // regular schedule mode
            int position = 0;
            // header
            dataset.add(getNewItem(TYPE_HEADER, new JSONObject()
                    .put("title", scheduleLessons.getScheduleHeader(activity, json.getString("title"), json.getString("type")))
                    .put("week", scheduleLessons.getScheduleWeek(activity, weekday))
            ));
            position++;
            // schedule
            final JSONArray schedule = json.getJSONArray("schedule");
            int lessons_count = 0;
            for (int i = 0; i < schedule.length(); i++) {
                final JSONObject day = schedule.getJSONObject(i);
                final int day_weekday = day.getInt("weekday");
                JSONArray lessons = day.getJSONArray("lessons");
                if (lessons.length() == 0) {
                    continue;
                }
                lessons = filterLessons(lessons, TYPE, "hidden".equals(reduced_lesson_mode));
                if (lessons.length() == 0) {
                    continue;
                }
                dataset.add(getNewItem(TYPE_DAY, new JSONObject().put("text", getDayTitle(day))));
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
                    Item item = getNewItem(type, lessons.getJSONObject(j));
                    item.extras.put("weekday", day_weekday);
                    dataset.add(item);
                    position++;
                    lessons_count++;
                }
            }
            if (lessons_count == 0) {
                dataset.add(getNewItem(TYPE_NO_LESSONS, null));
            } else {
                // notification
                Calendar calendar = time.getCalendar();
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                if (month == Calendar.AUGUST && day > 21 || month == Calendar.SEPTEMBER && day < 21 || month == Calendar.JANUARY && day > 14 || month == Calendar.FEBRUARY && day < 14) {
                    dataset.add(getNewItem(TYPE_NOTIFICATION, new JSONObject().put("text", activity.getString(R.string.schedule_lessons_unstable_warning))));
                }
                // update time
                dataset.add(getNewItem(TYPE_UPDATE_TIME, new JSONObject().put("text", activity.getString(R.string.update_date) + " " + time.getUpdateTime(activity, json.getLong("timestamp")))));
            }
        }
        // that's all
        return dataset;
    }
    @NonNull
    private String getDayTitle(JSONObject day) {
        switch (JsonUtils.getInt(day, "weekday", -1)) {
            case 0: return activity.getString(R.string.monday);
            case 1: return activity.getString(R.string.tuesday);
            case 2: return activity.getString(R.string.wednesday);
            case 3: return activity.getString(R.string.thursday);
            case 4: return activity.getString(R.string.friday);
            case 5: return activity.getString(R.string.saturday);
            case 6: return activity.getString(R.string.sunday);
            default:
                /*TODO implement when isu will be ready
                расписание из ису, когда есть расписания на определенный день
                String type = JsonUtils.getString(day, "type");
                if ("date".equals(type)) {
                    String title = JsonUtils.getString(day, "title");
                    if (StringUtils.isNotBlank(title)) {
                        return title;
                    }
                }
                */
                return activity.getString(R.string.unknown_day);
        }
    }
    private JSONArray filterLessons(JSONArray lessons, int week, boolean hideReducedLessons) {
        final JSONArray filtered = new JSONArray();
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = JsonUtils.getJsonObject(lessons, i);
            if (lesson == null) {
                continue;
            }
            int lWeek = JsonUtils.getInt(lesson, "week", -1);
            if (!(week == 2 || lWeek == 2 || week == lWeek)) {
                continue;
            }
            if (hideReducedLessons) {
                String cdoitmoType = JsonUtils.getString(lesson, "cdoitmo_type");
                if ("reduced".equals(cdoitmoType)) {
                    continue;
                }
            }
            filtered.put(lesson);
        }
        return filtered;
    }
    private String getLessonDesc(JSONObject lesson, String type) {
        switch (type) {
            case "group": return JsonUtils.getString(lesson, "teacher");
            case "teacher": return JsonUtils.getString(lesson, "group");
            case "mine":
            case "room": {
                String group = JsonUtils.getString(lesson, "group");
                String teacher = JsonUtils.getString(lesson, "teacher");
                if (StringUtils.isBlank(group)) {
                    return teacher;
                }
                String desc = group;
                if (StringUtils.isNotBlank(teacher)) {
                    desc += " (" + teacher + ")";
                }
                return desc;
            }
        }
        return null;
    }
    private String getLessonMeta(JSONObject lesson, String type) {
        switch (type) {
            case "mine":
            case "group":
            case "teacher": {
                String room = JsonUtils.getString(lesson, "room");
                String building = JsonUtils.getString(lesson, "building");
                if (StringUtils.isBlank(room)) {
                    return building;
                }
                String meta = activity.getString(R.string.room_short) + " " + room;
                if (StringUtils.isNotBlank(building)) {
                    meta += " (" + building + ")";
                }
                return meta;
            }
            case "room": {
                return JsonUtils.getString(lesson, "building");
            }
        }
        return null;
    }

    public int getDayPosition(int weekday) {
        return days_positions.get(weekday, -1);
    }

    private FrameLayout getFlag(String text, int textColor, int backgroundColor) {
        FrameLayout flagContainer = (FrameLayout) inflate(R.layout.layout_schedule_lessons_flag);
        if (flagContainer == null) {
            return null;
        }
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
    private View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
}
