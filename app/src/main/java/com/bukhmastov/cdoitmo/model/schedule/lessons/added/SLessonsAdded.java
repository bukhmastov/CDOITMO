package com.bukhmastov.cdoitmo.model.schedule.lessons.added;

import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;

import java.util.ArrayList;
import java.util.Objects;

public class SLessonsAdded extends ScheduleJsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("schedule")
    private ArrayList<SDay> schedule;

    public SLessonsAdded() {
        super();
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

    @Override
    public boolean isEmptySchedule() {
        return schedule == null || schedule.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SLessonsAdded)) return false;
        SLessonsAdded that = (SLessonsAdded) o;
        return timestamp == that.timestamp &&
                Objects.equals(schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, schedule);
    }

    @Override
    public String toString() {
        return "SLessonsAdded{" +
                "timestamp=" + timestamp +
                ", schedule=" + schedule +
                '}';
    }
}
