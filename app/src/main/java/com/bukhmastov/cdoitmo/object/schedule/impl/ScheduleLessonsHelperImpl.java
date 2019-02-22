package com.bukhmastov.cdoitmo.object.schedule.impl;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsModifyFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsModifyFragmentPresenter;
import com.bukhmastov.cdoitmo.function.Callable;
import com.bukhmastov.cdoitmo.function.Consumer;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.lessons.added.SLessonsAdded;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SDayReduced;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SLessonsReduced;
import com.bukhmastov.cdoitmo.object.schedule.ScheduleLessonsHelper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;

import javax.inject.Inject;

public class ScheduleLessonsHelperImpl implements ScheduleLessonsHelper {

    private static final String TAG = "ScheduleLessonsHelper";

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    Context context;
    @Inject
    Storage storage;
    @Inject
    TextUtils textUtils;
    @Inject
    Time time;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public ScheduleLessonsHelperImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public boolean clearChanges(String query, Callable callback) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query)) {
                return false;
            }
            log.v(TAG, "clearChanges | query=", query);
            String token = query.toLowerCase();
            boolean added = false;
            boolean reduced = false;
            if (storage.exists(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token)) {
                added = storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added");
                }
            }
            if (storage.exists(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token)) {
                reduced = storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced");
                }
            }
            if (callback != null && (added || reduced)) {
                callback.call();
            }
            return added || reduced;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean reduceLesson(String query, int weekday, SLesson lesson, Callable callback) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query) || lesson == null) {
                return false;
            }
            log.v(TAG, "reduceLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            if (!Objects.equals(lesson.getCdoitmoType(), "normal")) {
                throw new Exception("Wrong cdoitmo_type type: " + lesson.getCdoitmoType());
            }
            String token = query.toLowerCase();
            String lessonHash = getLessonHash(lesson);
            String reduced = storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, null);
            SLessonsReduced lessonsReduced = new SLessonsReduced();
            if (StringUtils.isNotBlank(reduced)) {
                lessonsReduced.fromJsonString(reduced);
            }
            if (lessonsReduced.getSchedule() == null) {
                lessonsReduced.setSchedule(new ArrayList<>());
            }
            boolean found = false;
            for (SDayReduced dayReduced : lessonsReduced.getSchedule()) {
                if (dayReduced.getWeekday() != weekday) {
                    continue;
                }
                boolean foundLesson = false;
                for (String hash : dayReduced.getLessons()) {
                    if (Objects.equals(lessonHash, hash)) {
                        foundLesson = true;
                        break;
                    }
                }
                if (!foundLesson) {
                    dayReduced.getLessons().add(lessonHash);
                }
                found = true;
                break;
            }
            if (!found) {
                SDayReduced dayReduced = new SDayReduced();
                dayReduced.setWeekday(weekday);
                dayReduced.setLessons(new ArrayList<>());
                dayReduced.getLessons().add(lessonHash);
                lessonsReduced.getSchedule().add(dayReduced);
            }
            lessonsReduced.setTimestamp(time.getTimeInMillis());
            storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, lessonsReduced.toJsonString());
            if (callback != null) {
                callback.call();
            }
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_REDUCE,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lesson.getSubject())
            );
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean restoreLesson(String query, int weekday, SLesson lesson, Callable callback) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query) || lesson == null) {
                return false;
            }
            log.v(TAG, "restoreLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            if (!Objects.equals(lesson.getCdoitmoType(), "reduced")) {
                throw new Exception("Wrong cdoitmo_type type: " + lesson.getCdoitmoType());
            }
            String token = query.toLowerCase();
            String lessonHash = getLessonHash(lesson);
            String reduced = storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, null);
            if (StringUtils.isBlank(reduced)) {
                return false;
            }
            SLessonsReduced lessonsReduced = new SLessonsReduced().fromJsonString(reduced);
            if (lessonsReduced == null || CollectionUtils.isEmpty(lessonsReduced.getSchedule())) {
                return false;
            }
            for (SDayReduced dayReduced : lessonsReduced.getSchedule()) {
                if (dayReduced.getWeekday() != weekday) {
                    continue;
                }
                for (String hash : dayReduced.getLessons()) {
                    if (Objects.equals(lessonHash, hash)) {
                        dayReduced.getLessons().remove(hash);
                        break;
                    }
                }
                if (dayReduced.getLessons().size() == 0) {
                    lessonsReduced.getSchedule().remove(dayReduced);
                }
                break;
            }
            if (lessonsReduced.getSchedule().size() == 0) {
                storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced");
                }
            } else {
                lessonsReduced.setTimestamp(time.getTimeInMillis());
                storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, lessonsReduced.toJsonString());
            }
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean createLesson(String query, String title, String type, int weekday, SLesson lesson, Consumer<Bundle> onOpen) {
        try {
            if (StringUtils.isBlank(query) || StringUtils.isBlank(title) || StringUtils.isBlank(type) || lesson == null) {
                return false;
            }
            log.v(TAG, "createLesson | open fragment | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            Bundle extras = new Bundle();
            extras.putString("action_type", ScheduleLessonsModifyFragmentPresenter.CREATE);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putSerializable("lesson", lesson);
            onOpen.accept(extras);
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean createLesson(String query, int weekday, SLesson lesson, Callable callback) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query) || lesson == null) {
                return false;
            }
            log.v(TAG, "createLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            lesson.setCdoitmoType("synthetic");
            String token = query.toLowerCase();
            String added = storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, null);
            SLessonsAdded lessonsAdded = new SLessonsAdded();
            if (StringUtils.isNotBlank(added)) {
                lessonsAdded.fromJsonString(added);
            }
            if (lessonsAdded.getSchedule() == null) {
                lessonsAdded.setSchedule(new ArrayList<>());
            }
            boolean found = false;
            for (SDay day : lessonsAdded.getSchedule()) {
                if (day.getWeekday() != weekday) {
                    continue;
                }
                day.getLessons().add(lesson);
                found = true;
            }
            if (!found) {
                SDay day = new SDay();
                day.setWeekday(weekday);
                day.setLessons(new ArrayList<>());
                day.getLessons().add(lesson);
                lessonsAdded.getSchedule().add(day);
            }
            lessonsAdded.setTimestamp(time.getTimeInMillis());
            storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, lessonsAdded.toJsonString());
            firebaseAnalyticsProvider.logEvent(
                    context,
                    FirebaseAnalyticsProvider.Event.SCHEDULE_LESSON_ADD,
                    firebaseAnalyticsProvider.getBundle(FirebaseAnalyticsProvider.Param.LESSON_TITLE, lesson.getSubject())
            );
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean deleteLesson(String query, int weekday, SLesson lesson, Callable callback) {
        try {
            thread.assertNotUI();
            if (StringUtils.isBlank(query) || lesson == null) {
                return false;
            }
            log.v(TAG, "deleteLesson | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            if (!Objects.equals(lesson.getCdoitmoType(), "synthetic")) {
                throw new Exception("Wrong cdoitmo_type type: " + lesson.getCdoitmoType());
            }
            String lessonHash = getLessonHash(lesson);
            String token = query.toLowerCase();
            String added = storage.get(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, null);
            if (StringUtils.isBlank(added)) {
                return false;
            }
            SLessonsAdded lessonsAdded = new SLessonsAdded().fromJsonString(added);
            if (lessonsAdded == null || CollectionUtils.isEmpty(lessonsAdded.getSchedule())) {
                return false;
            }
            for (Iterator<SDay> it = lessonsAdded.getSchedule().iterator(); it.hasNext(); ) {
                SDay day = it.next();
                if (day.getWeekday() != weekday) {
                    continue;
                }
                for (SLesson sLesson : day.getLessons()) {
                    if (Objects.equals(getLessonHash(sLesson), lessonHash)) {
                        day.getLessons().remove(sLesson);
                        break;
                    }
                }
                if (day.getLessons().size() == 0) {
                    it.remove();
                }
            }
            if (lessonsAdded.getSchedule().size() == 0) {
                storage.delete(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token);
                if (storage.list(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added").size() == 0) {
                    storage.clear(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added");
                }
            } else {
                lessonsAdded.setTimestamp(time.getTimeInMillis());
                storage.put(context, Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, lessonsAdded.toJsonString());
            }
            if (callback != null) {
                callback.call();
            }
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public boolean editLesson(String query, String title, String type, int weekday, SLesson lesson, Consumer<Bundle> onOpen) {
        try {
            if (StringUtils.isBlank(query) || StringUtils.isBlank(title) || StringUtils.isBlank(type) || lesson == null) {
                return false;
            }
            log.v(TAG, "editLesson | open fragment | query=", query, " | weekday=", weekday, " | lesson=", lesson);
            Bundle extras = new Bundle();
            extras.putString("action_type", ScheduleLessonsModifyFragmentPresenter.EDIT);
            extras.putString("query", query);
            extras.putString("type", type);
            extras.putString("title", title);
            extras.putInt("weekday", weekday);
            extras.putSerializable("lesson", lesson);
            onOpen.accept(extras);
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    @Override
    public String getLessonHash(SLesson lesson) throws Exception {
        return textUtils.crypt(getLessonSignature(lesson));
    }

    @Override
    public String getLessonSignature(SLesson lesson) throws Exception {
        SLesson replica = new SLesson();
        replica.setSubject(lesson.getSubjectWithNote());
        replica.setType(lesson.getType());
        replica.setParity(lesson.getParity());
        replica.setTimeStart(lesson.getTimeStart());
        replica.setTimeEnd(lesson.getTimeEnd());
        replica.setGroup(lesson.getGroup());
        replica.setTeacherName(lesson.getTeacherName());
        replica.setTeacherId(lesson.getTeacherId());
        replica.setRoom(lesson.getRoom());
        replica.setBuilding(lesson.getBuilding());
        return replica.toJsonString();
    }

    @Override
    public TreeSet<SLesson> filterAndSortLessons(Collection<SLesson> lessons, int parity, boolean hideReducedLessons) {
        TreeSet<SLesson> filtered = new TreeSet<>(SLesson::compareTo);
        if (CollectionUtils.isEmpty(lessons)) {
            return filtered;
        }
        for (SLesson lesson : lessons) {
            if (lesson == null) {
                continue;
            }
            if (!(parity == 2 || lesson.getParity() == 2 || parity == lesson.getParity())) {
                continue;
            }
            if (hideReducedLessons && "reduced".equals(lesson.getCdoitmoType())) {
                continue;
            }
            filtered.add(lesson);
        }
        return filtered;
    }

    @Override
    public TreeSet<SDay> filterAndSortDays(Collection<SDay> days) {
        TreeSet<SDay> filtered = new TreeSet<>(SDay::compareTo);
        if (CollectionUtils.isEmpty(days)) {
            return filtered;
        }
        for (SDay day : days) {
            if (day == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            filtered.add(day);
        }
        return filtered;
    }

    @Override
    public TreeSet<SLesson> filterAndSortLessonsForWeekday(SLessons schedule, int parity, int weekday, boolean hideReducedLessons) {
        if (CollectionUtils.isEmpty(schedule.getSchedule())) {
            return new TreeSet<>();
        }
        for (SDay day : schedule.getSchedule()) {
            if (day == null || day.getWeekday() != weekday || CollectionUtils.isEmpty(day.getLessons())) {
                continue;
            }
            return filterAndSortLessons(day.getLessons(), parity, hideReducedLessons);
        }
        return new TreeSet<>();
    }
}
