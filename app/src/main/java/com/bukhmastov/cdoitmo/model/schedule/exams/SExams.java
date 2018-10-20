package com.bukhmastov.cdoitmo.model.schedule.exams;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;

import java.util.ArrayList;
import java.util.Objects;

public class SExams extends ScheduleJsonEntity {

    @JsonProperty("schedule_type")
    private String scheduleType;

    @JsonProperty("query")
    private String query;

    @JsonProperty("type")
    private String type;

    @JsonProperty("title")
    private String title;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("schedule")
    private ArrayList<SSubject> schedule;

    @JsonProperty("teachers")
    private STeachers teachers;

    public SExams() {
        super();
        scheduleType = "exams";
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

    public ArrayList<SSubject> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<SSubject> schedule) {
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
        if (!(o instanceof SExams)) return false;
        SExams sExams = (SExams) o;
        return timestamp == sExams.timestamp &&
                Objects.equals(scheduleType, sExams.scheduleType) &&
                Objects.equals(query, sExams.query) &&
                Objects.equals(type, sExams.type) &&
                Objects.equals(title, sExams.title) &&
                Objects.equals(schedule, sExams.schedule) &&
                Objects.equals(teachers, sExams.teachers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleType, query, type, title, timestamp, schedule, teachers);
    }

    @Override
    public String toString() {
        return "SExams{" +
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
