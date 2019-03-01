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
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

public class ScheduleLessonsIsuConverter extends Converter<ISUScheduleApiResponse, SLessons> {

    private String type = null;
    private String title = null;
    private static final Pattern SCHEDULE_CUSTOM_DAY_PATTERN = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");

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
                    if (Objects.equals(type, "group") && StringUtils.isBlank(title)) {
                        title = group;
                    }
                    for (ISUSchedule isuSchedule : CollectionUtils.emptyIfNull(isuGroup.getSchedule())) {
                        Integer weekday = isuSchedule.getWeekday();
                        if (weekday != null) {
                            weekday -= 1;
                            if (weekday < 0 || weekday > 6) {
                                weekday = null;
                            }
                        }
                        for (ISULesson isuLesson : CollectionUtils.emptyIfNull(isuSchedule.getLessons())) {
                            SLesson lesson = convertLesson(isuLesson, group);
                            putLessonToDay(days, weekday, lesson, isuLesson);
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
        lesson.setSubject(trim(isuLesson.getSubject()));
        lesson.setNote(trim(isuLesson.getNote()));
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
            case 2: parity = 0; break;
            case 1: parity = 1; break;
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
            if (Objects.equals(this.type, "teacher") && StringUtils.isBlank(title)) {
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

    private void putLessonToDay(List<SDay> days, Integer weekday, SLesson lesson, ISULesson isuLesson) {
        // ISU api provides no info about custom date, although not surprising
        // date_start always null, but maybe someday it wouldn't...
        String customDay = StringUtils.emptyIfNull(isuLesson.getDateStart());
        if (StringUtils.isNotBlank(customDay) && SCHEDULE_CUSTOM_DAY_PATTERN.matcher(customDay).matches()) {
            String[] parts = customDay.split("\\.");
            if (parts.length == 3) {
                Calendar calendar = time.get().getCalendar();
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]));
                calendar.set(Calendar.YEAR, Integer.parseInt(parts[2]));
                customDay = time.get().getScheduleCustomDayRaw(calendar);
            }
        }
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

    private String trim(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        value = StringUtils.removeHtmlTags(value);
        value = StringUtils.escapeString(value);
        if (".".equals(value) || ",".equals(value) || "-".equals(value)) {
            value = "";
        }
        return value;
    }
}
