package com.bukhmastov.cdoitmo.model.schedule.remote.itmo;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ITMOSLesson extends JsonEntity {

    /**
     * 0 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("data_day")
    private int weekday;

    /**
     * Тип занятия: "Лаб", "Прак", "Лек"
     */
    @JsonProperty("status")
    private String type;

    /**
     * 0 - Обе недели
     * 1 - Нечетная
     * 2 - Четная
     */
    @JsonProperty("data_week")
    private int parity;

    @JsonProperty("gr")
    private String group;

    /**
     * Формат: "08:20-09:50"
     */
    @JsonProperty("subj_time")
    private String time;

    @JsonProperty("start_time")
    private String timeStart;

    @JsonProperty("end_time")
    private String timeEnd;

    /**
     * Номер пары (в 08:20 значение 1, в 10:00 значение 2 и тд)
     */
    @JsonProperty("sortp")
    private int order;

    @JsonProperty("room")
    private String room;

    @JsonProperty("place")
    private String place;

    @JsonProperty("title")
    private String title;

    @JsonProperty("note")
    private String note;

    @JsonProperty("person")
    private String teacher;

    @JsonProperty("pid")
    private int teacherId;

    @JsonProperty("course")
    private int course;

    @JsonProperty("bld_id")
    private int buildingId;

    @JsonProperty("cathedra_bun_id")
    private int chairId;

    @JsonProperty("faculty_bun_id")
    private int facultyId;

    public ITMOSLesson() {
        super();
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public int getChairId() {
        return chairId;
    }

    public void setChairId(int chairId) {
        this.chairId = chairId;
    }

    public int getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(int facultyId) {
        this.facultyId = facultyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ITMOSLesson)) return false;
        ITMOSLesson that = (ITMOSLesson) o;
        return weekday == that.weekday &&
                parity == that.parity &&
                order == that.order &&
                teacherId == that.teacherId &&
                course == that.course &&
                buildingId == that.buildingId &&
                chairId == that.chairId &&
                facultyId == that.facultyId &&
                Objects.equals(type, that.type) &&
                Objects.equals(group, that.group) &&
                Objects.equals(time, that.time) &&
                Objects.equals(timeStart, that.timeStart) &&
                Objects.equals(timeEnd, that.timeEnd) &&
                Objects.equals(room, that.room) &&
                Objects.equals(place, that.place) &&
                Objects.equals(title, that.title) &&
                Objects.equals(note, that.note) &&
                Objects.equals(teacher, that.teacher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, type, parity, group, time, timeStart, timeEnd, order, room, place, title, note, teacher, teacherId, course, buildingId, chairId, facultyId);
    }

    @Override
    public String toString() {
        return "ITMOSLesson{" +
                "weekday=" + weekday +
                ", type='" + type + '\'' +
                ", parity=" + parity +
                ", group='" + group + '\'' +
                ", time='" + time + '\'' +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                ", order=" + order +
                ", room='" + room + '\'' +
                ", place='" + place + '\'' +
                ", title='" + title + '\'' +
                ", note='" + note + '\'' +
                ", teacher='" + teacher + '\'' +
                ", teacherId=" + teacherId +
                ", course=" + course +
                ", buildingId=" + buildingId +
                ", chairId=" + chairId +
                ", facultyId=" + facultyId +
                '}';
    }
}
