package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISULesson extends JsonEntity {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("note")
    private String note;

    /**
     * Тип занятия:
     * 1 - Лекции
     * 2 - Лабораторные занятия
     * 3 - Практические занятия
     */
    @JsonProperty("type")
    private String type;

    @JsonProperty("type_name")
    private String typeName;

    @JsonProperty("time_start")
    private String timeStart;

    @JsonProperty("time_end")
    private String timeEnd;

    @JsonProperty("date_start")
    private String dateStart;

    @JsonProperty("date_end")
    private String dateEnd;

    /**
     * 0 - Обе недели
     * 1 - Нечетная
     * 2 - Четная
     */
    @JsonProperty("parity")
    private int parity;

    @JsonProperty("teachers")
    private ArrayList<ISUTeacher> teachers;

    @JsonProperty("auditories")
    private ArrayList<ISUAuditory> auditories;

    public ISULesson() {
        super();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
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
        if (!(o instanceof ISULesson)) return false;
        ISULesson isuLesson = (ISULesson) o;
        return parity == isuLesson.parity &&
                Objects.equals(subject, isuLesson.subject) &&
                Objects.equals(note, isuLesson.note) &&
                Objects.equals(type, isuLesson.type) &&
                Objects.equals(typeName, isuLesson.typeName) &&
                Objects.equals(timeStart, isuLesson.timeStart) &&
                Objects.equals(timeEnd, isuLesson.timeEnd) &&
                Objects.equals(dateStart, isuLesson.dateStart) &&
                Objects.equals(dateEnd, isuLesson.dateEnd) &&
                Objects.equals(teachers, isuLesson.teachers) &&
                Objects.equals(auditories, isuLesson.auditories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, note, type, typeName, timeStart, timeEnd, dateStart, dateEnd, parity, teachers, auditories);
    }

    @Override
    public String toString() {
        return "ISULesson{" +
                "subject='" + subject + '\'' +
                ", note='" + note + '\'' +
                ", type='" + type + '\'' +
                ", typeName='" + typeName + '\'' +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                ", dateStart='" + dateStart + '\'' +
                ", dateEnd='" + dateEnd + '\'' +
                ", parity=" + parity +
                ", teachers=" + teachers +
                ", auditories=" + auditories +
                '}';
    }
}
