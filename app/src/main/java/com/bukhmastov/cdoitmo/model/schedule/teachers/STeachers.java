package com.bukhmastov.cdoitmo.model.schedule.teachers;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;

import java.util.ArrayList;
import java.util.Objects;

public class STeachers extends ScheduleJsonEntity {

    @JsonProperty("lastname")
    private String query;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("count")
    private int count;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("datasource")
    private String dataSource;

    @JsonProperty("list")
    private ArrayList<STeacher> teachers;

    public STeachers() {
        super();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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

    public ArrayList<STeacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(ArrayList<STeacher> teachers) {
        this.teachers = teachers;
    }

    @Override
    public boolean isEmptySchedule() {
        return teachers == null || teachers.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof STeachers)) return false;
        STeachers sTeachers = (STeachers) o;
        return limit == sTeachers.limit &&
                offset == sTeachers.offset &&
                count == sTeachers.count &&
                timestamp == sTeachers.timestamp &&
                Objects.equals(query, sTeachers.query) &&
                Objects.equals(teachers, sTeachers.teachers) &&
                Objects.equals(dataSource, sTeachers.dataSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, limit, offset, count, timestamp, teachers, dataSource);
    }

    @Override
    public String toString() {
        return "STeachers{" +
                "query='" + query + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", count=" + count +
                ", timestamp=" + timestamp +
                ", dataSource='" + dataSource + '\'' +
                ", teachers=" + teachers +
                '}';
    }
}
