package com.bukhmastov.cdoitmo.object.impl;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessons;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

public class SettingsScheduleLessonsImpl extends SettingsSchedule<SLessons> implements SettingsScheduleLessons {

    private static final String TAG = "SettingsSL";

    @Inject
    ScheduleLessons scheduleLessons;

    public SettingsScheduleLessonsImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void show(ConnectedActivity activity, Preference preference, Consumer<String> callback) {
        super.show(activity, preference, callback);
    }

    @Override
    protected void search(String query) {
        log.v(TAG, "search | query=", query);
        scheduleLessons.search(query, this);
    }

    @Override
    public void onSuccess(SLessons schedule, boolean fromCache) {
        try {
            log.v(TAG, "search | onSuccess | schedule=", (schedule == null ? "null" : "notnull"));
            toggleSearchState("action");
            if (schedule == null || StringUtils.isBlank(schedule.getType())) {
                notificationMessage.snackBar(activity, activity.getString(R.string.schedule_not_found));
                return;
            }
            switch (schedule.getType()) {
                case "group":
                case "room":
                case "teacher": {
                    if (CollectionUtils.isEmpty(schedule.getSchedule())) {
                        return;
                    }
                    query = schedule.getQuery();
                    title = ("room".equals(schedule.getType()) ? activity.getString(R.string.room) + " " : "") + schedule.getTitle();
                    log.v(TAG, "search | onSuccess | done | query=", query, " | title=", title);
                    toggleSearchState("selected");
                    break;
                }
                case "teachers": {
                    teacherPickerAdapter.clear();
                    if (schedule.getTeachers() == null || CollectionUtils.isEmpty(schedule.getTeachers().getTeachers())) {
                        return;
                    }
                    ArrayList<STeacher> teachers = schedule.getTeachers().getTeachers();
                    log.v(TAG, "search | onSuccess | type=", schedule.getType(), " | length=", teachers.size());
                    if (teachers.size() == 1) {
                        STeacher teacher = teachers.get(0);
                        if (teacher == null) {
                            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                            return;
                        }
                        query = teacher.getPersonId();
                        title = teacher.getPerson();
                        log.v(TAG, "search | onSuccess | done | query=", query, " | title=", title);
                        thread.runOnUI(() -> searchTextView.setText(title));
                        toggleSearchState("selected");
                        return;
                    }
                    thread.runOnUI(() -> {
                        teacherPickerAdapter.addAll(teachers);
                        teacherPickerAdapter.addTeachers(teachers);
                        if (teachers.size() > 0) {
                            searchTextView.showDropDown();
                        }
                    });
                    break;
                }
                default: {
                    notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    break;
                }
            }
        } catch (Throwable throwable) {
            log.exception(throwable);
            notificationMessage.snackBar(activity, activity.getString(R.string.something_went_wrong));
        }
    }

    @Override
    protected String getHint() {
        return activity.getString(R.string.schedule_lessons_search_view_hint);
    }
}
