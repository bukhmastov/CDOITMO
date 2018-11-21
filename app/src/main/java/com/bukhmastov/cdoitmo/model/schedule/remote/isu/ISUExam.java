package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUExam extends JsonEntity {

    @JsonProperty("subject")
    private String subject;

    /**
     * Тип экзамена:
     * 5 - Экзамен
     * 6 - Зачет
     */
    @JsonProperty("type")
    private int type;

    @JsonProperty("type_name")
    private String typeName;

    @JsonProperty("exam_time")
    private String examTime;

    @JsonProperty("exam_date")
    private String examDate;

    /**
     * 0 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("exam_day")
    private int examDay;

    @JsonProperty("exam_day_text")
    private String examDayText;

    @JsonProperty("advice_time")
    private String adviceTime;

    @JsonProperty("advice_date")
    private String adviceDate;

    /**
     * 0 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("advice_day")
    private int adviceDay;

    @JsonProperty("advice_day_text")
    private String adviceDayText;

    @JsonProperty("teachers")
    private ArrayList<ISUTeacher> teachers;

    @JsonProperty("auditories")
    private ArrayList<ISUAuditory> auditories;

    public ISUExam() {
        super();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getExamTime() {
        return examTime;
    }

    public void setExamTime(String examTime) {
        this.examTime = examTime;
    }

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = examDate;
    }

    public int getExamDay() {
        return examDay;
    }

    public void setExamDay(int examDay) {
        this.examDay = examDay;
    }

    public String getExamDayText() {
        return examDayText;
    }

    public void setExamDayText(String examDayText) {
        this.examDayText = examDayText;
    }

    public String getAdviceTime() {
        return adviceTime;
    }

    public void setAdviceTime(String adviceTime) {
        this.adviceTime = adviceTime;
    }

    public String getAdviceDate() {
        return adviceDate;
    }

    public void setAdviceDate(String adviceDate) {
        this.adviceDate = adviceDate;
    }

    public int getAdviceDay() {
        return adviceDay;
    }

    public void setAdviceDay(int adviceDay) {
        this.adviceDay = adviceDay;
    }

    public String getAdviceDayText() {
        return adviceDayText;
    }

    public void setAdviceDayText(String adviceDayText) {
        this.adviceDayText = adviceDayText;
    }

    public ArrayList<ISUTeacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(ArrayList<ISUTeacher> teachers) {
        this.teachers = teachers;
    }

    public ArrayList<ISUAuditory> getAuditories() {
        return auditories;
    }

    public void setAuditories(ArrayList<ISUAuditory> auditories) {
        this.auditories = auditories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUExam)) return false;
        ISUExam isuExam = (ISUExam) o;
        return examDay == isuExam.examDay &&
                adviceDay == isuExam.adviceDay &&
                Objects.equals(subject, isuExam.subject) &&
                Objects.equals(type, isuExam.type) &&
                Objects.equals(typeName, isuExam.typeName) &&
                Objects.equals(examTime, isuExam.examTime) &&
                Objects.equals(examDate, isuExam.examDate) &&
                Objects.equals(examDayText, isuExam.examDayText) &&
                Objects.equals(adviceTime, isuExam.adviceTime) &&
                Objects.equals(adviceDate, isuExam.adviceDate) &&
                Objects.equals(adviceDayText, isuExam.adviceDayText) &&
                Objects.equals(teachers, isuExam.teachers) &&
                Objects.equals(auditories, isuExam.auditories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, type, typeName, examTime, examDate, examDay, examDayText, adviceTime, adviceDate, adviceDay, adviceDayText, teachers, auditories);
    }

    @Override
    public String toString() {
        return "ISUExam{" +
                "subject='" + subject + '\'' +
                ", type=" + type +
                ", typeName='" + typeName + '\'' +
                ", examTime='" + examTime + '\'' +
                ", examDate='" + examDate + '\'' +
                ", examDay=" + examDay +
                ", examDayText='" + examDayText + '\'' +
                ", adviceTime='" + adviceTime + '\'' +
                ", adviceDate='" + adviceDate + '\'' +
                ", adviceDay=" + adviceDay +
                ", adviceDayText='" + adviceDayText + '\'' +
                ", teachers=" + teachers +
                ", auditories=" + auditories +
                '}';
    }
}
