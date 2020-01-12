package com.bukhmastov.cdoitmo.model.schedule.lessons.reduced;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;

import java.util.ArrayList;
import java.util.Objects;

public class SLessonsReduced extends ScheduleJsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("schedule")
    private ArrayList<SDayReduced> schedule;

    public SLessonsReduced() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDataSource() {
        return "";
    }

    public ArrayList<SDayReduced> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<SDayReduced> schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean isEmptySchedule() {
        return schedule == null || schedule.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SLessonsReduced)) return false;
        SLessonsReduced that = (SLessonsReduced) o;
        return timestamp == that.timestamp &&
                Objects.equals(schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, schedule);
    }

    @Override
    public String toString() {
        return "SLessonsReduced{" +
                "timestamp=" + timestamp +
                ", schedule=" + schedule +
                '}';
    }
}
