package com.bukhmastov.cdoitmo.model.room101.requests;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RSession extends JsonEntity {

    @JsonProperty("number")
    private String number;

    @JsonProperty("date")
    private String date;

    @JsonProperty("time")
    private String time;

    @JsonProperty("timeStart")
    private String timeStart;

    @JsonProperty("timeEnd")
    private String timeEnd;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reid")
    private int reid;

    @JsonProperty("requested")
    private String requested;

    public RSession() {
        super();
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReid() {
        return reid;
    }

    public void setReid(int reid) {
        this.reid = reid;
    }

    public String getRequested() {
        return requested;
    }

    public void setRequested(String requested) {
        this.requested = requested;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RSession)) return false;
        RSession session = (RSession) o;
        return Objects.equals(number, session.number) &&
                Objects.equals(date, session.date) &&
                Objects.equals(time, session.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, date, time);
    }

    @Override
    public String toString() {
        return "RSession{" +
                "number='" + number + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", timeStart='" + timeStart + '\'' +
                ", timeEnd='" + timeEnd + '\'' +
                ", status='" + status + '\'' +
                ", reid=" + reid +
                ", requested='" + requested + '\'' +
                '}';
    }
}
