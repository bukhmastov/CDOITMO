package com.bukhmastov.cdoitmo.model.rating.pickerown;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class RatingPickerOwn extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("max_course")
    private int maxCourse;

    @JsonProperty("courses")
    private ArrayList<RCourse> courses;

    public RatingPickerOwn() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getMaxCourse() {
        return maxCourse;
    }

    public void setMaxCourse(int maxCourse) {
        this.maxCourse = maxCourse;
    }

    public ArrayList<RCourse> getCourses() {
        return courses;
    }

    public void setCourses(ArrayList<RCourse> courses) {
        this.courses = courses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RatingPickerOwn)) return false;
        RatingPickerOwn that = (RatingPickerOwn) o;
        return timestamp == that.timestamp &&
                maxCourse == that.maxCourse &&
                Objects.equals(courses, that.courses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, courses, maxCourse);
    }

    @Override
    public String toString() {
        return "RatingPickerOwn{" +
                "timestamp=" + timestamp +
                ", maxCourse=" + maxCourse +
                ", courses=" + courses +
                '}';
    }
}
