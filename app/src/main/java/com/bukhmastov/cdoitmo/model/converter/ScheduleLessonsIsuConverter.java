package com.bukhmastov.cdoitmo.model.converter;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUAuditory;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUDepartment;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUFaculty;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUGroup;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISULesson;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUSchedule;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUScheduleApiResponse;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUTeacher;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;

public class ScheduleLessonsIsuConverter extends Converter<ISUScheduleApiResponse, SLessons> {

    private String type = null;
    private String title = null;

    public ScheduleLessonsIsuConverter(@NonNull ISUScheduleApiResponse entity) {
        super(entity);
    }

    /**
     * One of [group, teacher]
     */
    public ScheduleLessonsIsuConverter setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    protected SLessons doConvert(ISUScheduleApiResponse entity) throws Throwable {
        ArrayList<SDay> days = new ArrayList<>();
        for (ISUFaculty isuFaculty : CollectionUtils.emptyIfNull(entity.getFaculties())) {
            for (ISUDepartment isuDepartment : CollectionUtils.emptyIfNull(isuFaculty.getDepartments())) {
                for (ISUGroup isuGroup : CollectionUtils.emptyIfNull(isuDepartment.getGroups())) {
                    String group = isuGroup.getGroup();
                    if (Objects.equals(type, "group")) {
                        title = group;
                    }
                    for (ISUSchedule isuSchedule : CollectionUtils.emptyIfNull(isuGroup.getSchedule())) {
                        int weekday = isuSchedule.getWeekday() - 1;
                        if (weekday < 0) {
                            weekday += 8;
                        }
                        for (ISULesson isuLesson : CollectionUtils.emptyIfNull(isuSchedule.getLessons())) {
                            SLesson lesson = convertLesson(isuLesson, group);
                            putLessonToDay(days, weekday, lesson);
                        }
                    }
                }
            }
        }
        SLessons schedule = new SLessons();
        schedule.setTitle(title);
        schedule.setSchedule(days);
        return schedule;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.LESSONS;
    }

    private SLesson convertLesson(ISULesson isuLesson, String group) {
        SLesson lesson = new SLesson();
        // subject + note
        lesson.setSubject(isuLesson.getSubject());
        lesson.setNote(isuLesson.getNote());
        // type
        String type = isuLesson.getType();
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "1": type = "lecture"; break;
                case "2": type = "lab"; break;
                case "3": type = "practice"; break;
                default: type = isuLesson.getTypeName(); break;
            }
        } else {
            type = isuLesson.getTypeName();
        }
        lesson.setType(type);
        // parity
        int parity = isuLesson.getParity();
        switch (parity) {
            case 2: parity = 1; break;
            case 1: parity = 0; break;
            case 0: default: parity = 2; break;
        }
        lesson.setParity(parity);
        // time
        lesson.setTimeStart(isuLesson.getTimeStart()); // "∞"
        lesson.setTimeEnd(isuLesson.getTimeEnd()); // "∞"
        // group
        lesson.setGroup(group);
        // teacher
        if (CollectionUtils.isNotEmpty(isuLesson.getTeachers())) {
            ISUTeacher isuTeacher = isuLesson.getTeachers().get(0);
            if (Objects.equals(type, "teacher")) {
                title = isuTeacher.getTeacherName();
            }
            lesson.setTeacherName(isuTeacher.getTeacherName());
            lesson.setTeacherId(isuTeacher.getTeacherId() == 0 ? null : String.valueOf(isuTeacher.getTeacherId()));
        }
        // place
        if (CollectionUtils.isNotEmpty(isuLesson.getAuditories())) {
            ISUAuditory isuAuditory = isuLesson.getAuditories().get(0);
            lesson.setRoom(isuAuditory.getAuditoryName());
            lesson.setBuilding(isuAuditory.getAuditoryAddress());
        }
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
