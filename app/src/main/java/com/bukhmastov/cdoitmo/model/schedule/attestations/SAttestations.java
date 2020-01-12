package com.bukhmastov.cdoitmo.model.schedule.attestations;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;

import java.util.ArrayList;
import java.util.Objects;

public class SAttestations extends ScheduleJsonEntity {

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

    @JsonProperty("datasource")
    private String dataSource;

    @JsonProperty("schedule")
    private ArrayList<SSubject> schedule;

    public SAttestations() {
        super();
        scheduleType = "attestations";
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

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList<SSubject> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<SSubject> schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean isEmptySchedule() {
        return schedule == null || schedule.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SAttestations)) return false;
        SAttestations that = (SAttestations) o;
        return timestamp == that.timestamp &&
                Objects.equals(scheduleType, that.scheduleType) &&
                Objects.equals(query, that.query) &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(schedule, that.schedule) &&
                Objects.equals(dataSource, that.dataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleType, query, type, title, timestamp, schedule, dataSource);
    }

    @Override
    public String toString() {
        return "SAttestations{" +
                "scheduleType='" + scheduleType + '\'' +
                ", query='" + query + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", timestamp=" + timestamp +
                ", dataSource='" + dataSource + '\'' +
                ", schedule=" + schedule +
                '}';
    }
}
