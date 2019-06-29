package com.bukhmastov.cdoitmo.model.converter;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.lessons.added.SLessonsAdded;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SDayReduced;
import com.bukhmastov.cdoitmo.model.schedule.lessons.reduced.SLessonsReduced;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScheduleLessonsAdditionalConverter extends Converter<SLessons, SLessons> {

    public ScheduleLessonsAdditionalConverter(@NonNull SLessons entity) {
        super(entity);
    }

    @Override
    protected SLessons doConvert(SLessons entity) throws Throwable {
        if (StringUtils.isBlank(entity.getQuery())) {
            return entity;
        }
        String token = entity.getQuery().toLowerCase();
        String added = storage.get().get(context.get(), Storage.PERMANENT, Storage.USER, "schedule_lessons#added#" + token, null);
        String reduced = storage.get().get(context.get(), Storage.PERMANENT, Storage.USER, "schedule_lessons#reduced#" + token, null);
        if (StringUtils.isNotBlank(reduced)) {
            SLessonsReduced lessonsReduced = new SLessonsReduced().fromJsonString(reduced);
            if (lessonsReduced != null && CollectionUtils.isNotEmpty(lessonsReduced.getSchedule())) {
                for (SDay day : entity.getSchedule()) {
                    for (SLesson lesson : day.getLessons()) {
                        applyReducedIfNeeded(lesson, day.getWeekday(), day.getTitle(), lessonsReduced);
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(added)) {
            SLessonsAdded lessonsAdded = new SLessonsAdded().fromJsonString(added);
            if (lessonsAdded != null && CollectionUtils.isNotEmpty(lessonsAdded.getSchedule())) {
                ArrayList<SDay> days = entity.getSchedule();
                for (SDay day : lessonsAdded.getSchedule()) {
                    for (SLesson lesson : day.getLessons()) {
                        putLessonToDay(days, day.getWeekday(), day.getTitle(), lesson);
                    }
                }
            }
        }
        return entity;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.ADDITIONAL;
    }

    private void putLessonToDay(List<SDay> days, Integer weekday, String customDay, SLesson lesson) {
        for (SDay day : days) {
            if (day.isMatched(weekday, customDay)) {
                day.addLesson(lesson);
                return;
            }
        }
        if (weekday != null) {
            days.add(new SDay(weekday, lesson));
        } else {
            days.add(new SDay(customDay, lesson));
        }
    }

    private void applyReducedIfNeeded(SLesson lesson, Integer weekday, String customDay, SLessonsReduced lessonsReduced) throws Throwable {
        if (Objects.equals(lesson.getCdoitmoType(), "synthetic")) {
            return;
        }
        String lessonHash = scheduleLessonsHelper.get().getLessonHash(lesson);
        for (SDayReduced dayReduced : lessonsReduced.getSchedule()) {
            if (!dayReduced.isMatched(weekday, customDay)) {
                continue;
            }
            for (String hash : dayReduced.getLessons()) {
                if (Objects.equals(lessonHash, hash)) {
                    lesson.setCdoitmoType("reduced");
                    return;
                }
            }
        }
    }
}
