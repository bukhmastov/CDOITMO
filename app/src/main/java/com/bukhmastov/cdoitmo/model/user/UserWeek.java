package com.bukhmastov.cdoitmo.model.user;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class UserWeek extends JsonEntity {

    @JsonProperty("week")
    private int week;

    @JsonProperty("timestamp")
    private long timestamp;

    public UserWeek() {
        super();
    }

    public UserWeek(int week, long timestamp) {
        super();
        setWeek(week);
        setTimestamp(timestamp);
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserWeek)) return false;
        UserWeek userWeek = (UserWeek) o;
        return week == userWeek.week &&
                timestamp == userWeek.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(week, timestamp);
    }

    @Override
    public String toString() {
        return "UserWeek{" +
                "week=" + week +
                ", timestamp=" + timestamp +
                '}';
    }
}
