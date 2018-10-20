package com.bukhmastov.cdoitmo.model.schedule.exams;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SExam extends JsonEntity {

    @JsonProperty("date")
    private String date;

    @JsonProperty("time")
    private String time;

    @JsonProperty("room")
    private String room;

    @JsonProperty("building")
    private String building;

    public SExam() {
        super();
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SExam)) return false;
        SExam sExam = (SExam) o;
        return Objects.equals(date, sExam.date) &&
                Objects.equals(time, sExam.time) &&
                Objects.equals(room, sExam.room) &&
                Objects.equals(building, sExam.building);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, time, room, building);
    }

    @Override
    public String toString() {
        return "SExam{" +
                "date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", room='" + room + '\'' +
                ", building='" + building + '\'' +
                '}';
    }
}
