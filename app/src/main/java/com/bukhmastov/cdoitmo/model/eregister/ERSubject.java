package com.bukhmastov.cdoitmo.model.eregister;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;

import java.util.ArrayList;
import java.util.Objects;

public class ERSubject extends JsonEntity implements Comparable<ERSubject> {

    @JsonProperty("name")
    private String name;

    @JsonProperty("semester")
    private String term;

    @JsonProperty("marks")
    private ArrayList<ERMark> marks;

    @JsonProperty("points")
    private ArrayList<ERPoint> points;

    public ERSubject() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTerm() {
        return NumberUtils.toInteger(term);
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public ArrayList<ERMark> getMarks() {
        return marks;
    }

    public void setMarks(ArrayList<ERMark> marks) {
        this.marks = marks;
    }

    public ArrayList<ERPoint> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<ERPoint> points) {
        this.points = points;
    }

    @Override
    public int compareTo(ERSubject subject) {
        int c = 0;
        if (getTerm() != null) {
            c = getTerm().compareTo(subject.getTerm());
        }
        if (c == 0 && getName() != null) {
            c = getName().compareTo(subject.getName());
        }
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ERSubject)) return false;
        ERSubject erSubject = (ERSubject) o;
        return Objects.equals(name, erSubject.name) &&
                Objects.equals(term, erSubject.term) &&
                Objects.equals(marks, erSubject.marks) &&
                Objects.equals(points, erSubject.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, term, marks, points);
    }

    @Override
    public String toString() {
        return "ERSubject{" +
                "name='" + name + '\'' +
                ", term='" + term + '\'' +
                ", marks=" + marks +
                ", points=" + points +
                '}';
    }
}
