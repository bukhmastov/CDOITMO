package com.bukhmastov.cdoitmo.model.converter;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.remote.itmo.ITMOSLesson;
import com.bukhmastov.cdoitmo.model.schedule.remote.itmo.ITMOSLessons;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;

public class ScheduleLessonsItmoConverter extends Converter<ITMOSLessons, SLessons> {

    public ScheduleLessonsItmoConverter(@NonNull ITMOSLessons entity) {
        super(entity);
    }

    @Override
    protected SLessons doConvert(ITMOSLessons entity) {
        ArrayList<SDay> days = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(entity.getSchedule())) {
            for (ITMOSLesson itmoLesson : entity.getSchedule()) {
                int weekday = itmoLesson.getWeekday();
                SLesson lesson = convertLesson(itmoLesson);
                putLessonToDay(days, weekday, lesson);
            }
        }
        SLessons schedule = new SLessons();
        schedule.setTitle(entity.getLabel());
        schedule.setSchedule(days);
        return schedule;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.LESSONS;
    }

    private SLesson convertLesson(ITMOSLesson itmoLesson) {
        SLesson lesson = new SLesson();
        // subject + note
        lesson.setSubject(itmoLesson.getTitle());
        lesson.setNote(itmoLesson.getNote());
        // type
        String type = itmoLesson.getType();
        if (StringUtils.isNotBlank(type)) {
            type = type.toLowerCase();
            if (type.startsWith("лек")) {
                type = "lecture";
            } else if (type.startsWith("прак")) {
                type = "practice";
            } else if (type.startsWith("лаб")) {
                type = "lab";
            } else if (type.equals("срс")) {
                type = "iws";
            }
        }
        lesson.setType(type);
        // parity
        int parity = itmoLesson.getParity();
        switch (parity) {
            case 2: parity = 1; break;
            case 1: parity = 0; break;
            case 0: default: parity = 2; break;
        }
        lesson.setParity(parity);
        // time
        lesson.setTimeStart(itmoLesson.getTimeStart()); // "∞"
        lesson.setTimeEnd(itmoLesson.getTimeEnd()); // "∞"
        // group
        lesson.setGroup(itmoLesson.getGroup());
        // teacher
        lesson.setTeacherName(itmoLesson.getTeacher());
        lesson.setTeacherId(itmoLesson.getTeacherId() == 0 ? null : String.valueOf(itmoLesson.getTeacherId()));
        // place
        lesson.setRoom(itmoLesson.getRoom());
        lesson.setBuilding(itmoLesson.getPlace());
        // in app type
        lesson.setCdoitmoType("normal");
        return lesson;
    }

    private void putLessonToDay(ArrayList<SDay> days, int weekday, SLesson lesson) {
        for (SDay day : days) {
            if (day.getWeekday() == weekday) {
                day.getLessons().add(lesson);
                return;
            }
        }
        SDay day = new SDay();
        day.setWeekday(weekday);
        day.setType("unknown");
        day.setTitle("");
        day.setLessons(new ArrayList<>());
        day.getLessons().add(lesson);
        days.add(day);
    }
}
