package com.bukhmastov.cdoitmo.model.rating.pickerown;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RCourse extends JsonEntity {

    @JsonProperty("faculty")
    private String faculty;

    @JsonProperty("course")
    private int course;

    @JsonProperty("position")
    private String position;

    public RCourse() {
        super();
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RCourse)) return false;
        RCourse course1 = (RCourse) o;
        return course == course1.course &&
                Objects.equals(faculty, course1.faculty) &&
                Objects.equals(position, course1.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(faculty, course, position);
    }

    @Override
    public String toString() {
        return "RCourse{" +
                "faculty='" + faculty + '\'' +
                ", course=" + course +
                ", position='" + position + '\'' +
                '}';
    }
}
