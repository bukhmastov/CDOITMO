package com.bukhmastov.cdoitmo.model.converter;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExam;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUAuditory;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUDepartment;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUExam;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUFaculty;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUGroup;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUScheduleApiResponse;
import com.bukhmastov.cdoitmo.model.schedule.remote.isu.ISUTeacher;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;

public class ScheduleExamsIsuConverter extends Converter<ISUScheduleApiResponse, SExams> {

    private String type = null;
    private String title = null;

    public ScheduleExamsIsuConverter(@NonNull ISUScheduleApiResponse entity) {
        super(entity);
    }

    /**
     * One of [group, teacher]
     */
    public ScheduleExamsIsuConverter setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    protected SExams doConvert(ISUScheduleApiResponse entity) throws Throwable {
        ArrayList<SSubject> schedule = new ArrayList<>();
        for (ISUFaculty isuFaculty : CollectionUtils.emptyIfNull(entity.getFaculties())) {
            for (ISUDepartment isuDepartment : CollectionUtils.emptyIfNull(isuFaculty.getDepartments())) {
                for (ISUGroup isuGroup : CollectionUtils.emptyIfNull(isuDepartment.getGroups())) {
                    String group = isuGroup.getGroup();
                    if (Objects.equals(type, "group")) {
                        title = group;
                    }
                    for (ISUExam isuExam : CollectionUtils.emptyIfNull(isuGroup.getExams())) {
                        SSubject subject = convertExam(isuExam, group);
                        schedule.add(subject);
                    }
                }
            }
        }
        SExams exams = new SExams();
        exams.setTitle(title);
        exams.setSchedule(schedule);
        return exams;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.Schedule.EXAMS;
    }

    private SSubject convertExam(ISUExam isuExam, String group) {
        SSubject subject = new SSubject();
        subject.setType(getType(isuExam.getType()));
        subject.setSubject(isuExam.getSubject());
        subject.setGroup(group);
        if (CollectionUtils.isNotEmpty(isuExam.getTeachers())) {
            ISUTeacher isuTeacher = isuExam.getTeachers().get(0);
            if (Objects.equals(type, "teacher")) {
                title = isuTeacher.getTeacherName();
            }
            subject.setTeacherName(isuTeacher.getTeacherName());
            subject.setTeacherId(isuTeacher.getTeacherId() == 0 ? null : String.valueOf(isuTeacher.getTeacherId()));
        }
        SExam exam = new SExam();
        exam.setTime(isuExam.getExamTime());
        exam.setDate(isuExam.getExamDate());
        if (CollectionUtils.isNotEmpty(isuExam.getAuditories())) {
            for (ISUAuditory isuAuditory : isuExam.getAuditories()) {
                if ("exam".equals(isuAuditory.getType())) {
                    exam.setRoom(isuAuditory.getAuditoryName());
                    exam.setBuilding(isuAuditory.getAuditoryAddress());
                    break;
                }
            }
        }
        subject.setExam(exam);
        SExam advice = new SExam();
        advice.setTime(isuExam.getAdviceTime());
        advice.setDate(isuExam.getAdviceDate());
        if (CollectionUtils.isNotEmpty(isuExam.getAuditories())) {
            for (ISUAuditory isuAuditory : isuExam.getAuditories()) {
                if ("advice".equals(isuAuditory.getType())) {
                    advice.setRoom(isuAuditory.getAuditoryName());
                    advice.setBuilding(isuAuditory.getAuditoryAddress());
                    break;
                }
            }
        }
        subject.setAdvice(advice);
        return subject;
    }

    private String getType(int type) {
        if (type == 9) {
            return "diffcredit";
        }
        if (type == 6) {
            return "credit";
        }
        return "exam";
    }
}
