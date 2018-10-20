package com.bukhmastov.cdoitmo.model.schedule.exams;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SSubject extends JsonEntity {

    /**
     * exam - экзамен
     * credit - зачет
     */
    @JsonProperty("type")
    private String type;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("group")
    private String group;

    @JsonProperty("teacher")
    private String teacherName;

    @JsonProperty("teacher_id")
    private String teacherId;

    @JsonProperty("exam")
    private SExam exam;

    @JsonProperty("advice")
    private SExam advice;

    public SSubject() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public SExam getExam() {
        return exam;
    }

    public void setExam(SExam exam) {
        this.exam = exam;
    }

    public SExam getAdvice() {
        return advice;
    }

    public void setAdvice(SExam advice) {
        this.advice = advice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSubject)) return false;
        SSubject sSubject = (SSubject) o;
        return Objects.equals(type, sSubject.type) &&
                Objects.equals(subject, sSubject.subject) &&
                Objects.equals(group, sSubject.group) &&
                Objects.equals(teacherName, sSubject.teacherName) &&
                Objects.equals(teacherId, sSubject.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subject, group, teacherName, teacherId);
    }

    @Override
    public String toString() {
        return "SSubject{" +
                "type='" + type + '\'' +
                ", subject='" + subject + '\'' +
                ", group='" + group + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", exam=" + exam +
                ", advice=" + advice +
                '}';
    }
}
