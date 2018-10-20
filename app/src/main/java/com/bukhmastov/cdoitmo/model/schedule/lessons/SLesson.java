package com.bukhmastov.cdoitmo.model.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SLesson extends JsonEntity {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("note")
    private String note;

    /**
     * lecture, practice, lab, iws
     */
    @JsonProperty("type")
    private String type;

    /**
     * 0 - Нечетная
     * 1 - Четная
     * 2 - Обе недели
     */
    @JsonProperty("week")
    private int parity;

    @JsonProperty("timeStart")
    private String timeStart;

    @JsonProperty("timeEnd")
    private String timeEnd;

    @JsonProperty("group")
    private String group;

    @JsonProperty("teacher")
    private String teacherName;

    @JsonProperty("teacher_id")
    private String teacherId;

    @JsonProperty("room")
    private String room;

    @JsonProperty("building")
    private String building;

    @JsonProperty("cdoitmo_type")
    private String cdoitmoType;

    public SLesson() {
        super();
    }

    public SLesson(SLesson lesson) {
        super();
        setSubject(lesson.getSubject());
        setNote(lesson.getNote());
        setType(lesson.getType());
        setParity(lesson.getParity());
        setTimeStart(lesson.getTimeStart());
        setTimeEnd(lesson.getTimeEnd());
        setGroup(lesson.getGroup());
        setTeacherName(lesson.getTeacherName());
        setTeacherId(lesson.getTeacherId());
        setRoom(lesson.getRoom());
        setBuilding(lesson.getBuilding());
        setCdoitmoType(lesson.getCdoitmoType());
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

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        this.parity = parity;
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getCdoitmoType() {
        return cdoitmoType;
    }

    public void setCdoitmoType(String cdoitmoType) {
        this.cdoitmoType = cdoitmoType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SLesson)) return false;
        SLesson sLesson = (SLesson) o;
        return parity == sLesson.parity &&
                Objects.equals(subject, sLesson.subject) &&
                Objects.equals(note, sLesson.note) &&
                Objects.equals(type, sLesson.type) &&
                Objects.equals(timeStart, sLesson.timeStart) &&
                Objects.equals(timeEnd, sLesson.timeEnd) &&
                Objects.equals(group, sLesson.group) &&
                Objects.equals(teacherName, sLesson.teacherName) &&
                Objects.equals(teacherId, sLesson.teacherId) &&
                Objects.equals(room, sLesson.room) &&
                Objects.equals(building, sLesson.building) &&
                Objects.equals(cdoitmoType, sLesson.cdoitmoType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, note, type, parity, timeStart, timeEnd, group, teacherName, teacherId, room, building, cdoitmoType);
    }

    @Override
    public String toString() {
        return "SLesson{" +
                "subject='" + subject + '\'' +
                ", note='" + note + '\'' +
                ", type='" + type + '\'' +
                ", parity=" + parity +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                ", group='" + group + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", teacherId='" + teacherId + '\'' +
                ", room='" + room + '\'' +
                ", building='" + building + '\'' +
                ", cdoitmoType='" + cdoitmoType + '\'' +
                '}';
    }
}
