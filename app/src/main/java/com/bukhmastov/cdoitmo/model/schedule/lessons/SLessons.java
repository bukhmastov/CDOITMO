package com.bukhmastov.cdoitmo.model.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;

import java.util.ArrayList;
import java.util.Objects;

public class SLessons extends ScheduleJsonEntity {

    /**
     * Общий тип расписания: "lessons", "exams", "attestations"
     */
    @JsonProperty("schedule_type")
    private String scheduleType;

    /**
     * Строка, по которой идет поиск: "personal", "K3320", "123456", "336", "Зинчик" (Используется для повторного поиска и кэширования)
     */
    @JsonProperty("query")
    private String query;

    /**
     * Тип расписания: "personal", "group", "room", "teacher", "teachers"
     */
    @JsonProperty("type")
    private String type;

    /**
     * Заголовок расписания: "K3320", "336", "Зинчик Зинчик Зинчикович"
     */
    @JsonProperty("title")
    private String title;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("schedule")
    private ArrayList<SDay> schedule;

    @JsonProperty("teachers")
    private STeachers teachers;

    public SLessons() {
        super();
        scheduleType = "lessons";
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<SDay> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<SDay> schedule) {
        this.schedule = schedule;
    }

    public STeachers getTeachers() {
        return teachers;
    }

    public void setTeachers(STeachers teachers) {
        this.teachers = teachers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SLessons)) return false;
        SLessons that = (SLessons) o;
        return timestamp == that.timestamp &&
                Objects.equals(scheduleType, that.scheduleType) &&
                Objects.equals(query, that.query) &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(schedule, that.schedule) &&
                Objects.equals(teachers, that.teachers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleType, query, type, title, timestamp, schedule, teachers);
    }

    @Override
    public String toString() {
        return "SLessons{" +
                "scheduleType='" + scheduleType + '\'' +
                ", query='" + query + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", timestamp=" + timestamp +
                ", schedule=" + schedule +
                ", teachers=" + teachers +
                '}';
    }
}
