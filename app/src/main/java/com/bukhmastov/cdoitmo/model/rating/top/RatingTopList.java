package com.bukhmastov.cdoitmo.model.rating.top;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class RatingTopList extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("header")
    private String header;

    @JsonProperty("list")
    private ArrayList<RStudent> students;

    public RatingTopList() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public ArrayList<RStudent> getStudents() {
        return students;
    }

    public void setStudents(ArrayList<RStudent> students) {
        this.students = students;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RatingTopList)) return false;
        RatingTopList that = (RatingTopList) o;
        return timestamp == that.timestamp &&
                Objects.equals(header, that.header) &&
                Objects.equals(students, that.students);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, header, students);
    }

    @Override
    public String toString() {
        return "RatingTopList{" +
                "timestamp=" + timestamp +
                ", header='" + header + '\'' +
                ", students=" + students +
                '}';
    }
}
