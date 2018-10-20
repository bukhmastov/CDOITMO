package com.bukhmastov.cdoitmo.model.room101.requests;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class Room101Requests extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("date")
    private String date;

    @JsonProperty("limit")
    private String limit;

    @JsonProperty("left")
    private String left;

    @JsonProperty("penalty")
    private String penalty;

    @JsonProperty("sessions")
    private ArrayList<RSession> sessions;

    public Room101Requests() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLimit() {
        return limit;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getPenalty() {
        return penalty;
    }

    public void setPenalty(String penalty) {
        this.penalty = penalty;
    }

    public ArrayList<RSession> getSessions() {
        return sessions;
    }

    public void setSessions(ArrayList<RSession> sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room101Requests)) return false;
        Room101Requests that = (Room101Requests) o;
        return timestamp == that.timestamp &&
                Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, date);
    }

    @Override
    public String toString() {
        return "Room101Requests{" +
                "timestamp=" + timestamp +
                ", date='" + date + '\'' +
                ", limit='" + limit + '\'' +
                ", left='" + left + '\'' +
                ", penalty='" + penalty + '\'' +
                ", sessions=" + sessions +
                '}';
    }
}
