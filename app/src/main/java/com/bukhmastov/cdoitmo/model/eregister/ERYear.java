package com.bukhmastov.cdoitmo.model.eregister;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

public class ERYear extends JsonEntity {

    @JsonProperty("group")
    private String group;

    @JsonProperty("studyyear")
    private String years;

    @JsonProperty("subjects")
    private ArrayList<ERSubject> subjects;

    public ERYear() {
        super();
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getYears() {
        return years;
    }

    public Integer getYearFirst() {
        if (StringUtils.isBlank(years)) {
            return null;
        }
        String[] y = years.split("/");
        if (y.length == 0) {
            return null;
        }
        try {
            if (y.length == 1) {
                return Integer.valueOf(y[0]);
            }
            return Math.min(Integer.valueOf(y[0]), Integer.valueOf(y[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getYearSecond() {
        if (StringUtils.isBlank(years)) {
            return null;
        }
        String[] y = years.split("/");
        if (y.length == 0) {
            return null;
        }
        try {
            if (y.length == 1) {
                return Integer.valueOf(y[0]);
            }
            return Math.max(Integer.valueOf(y[0]), Integer.valueOf(y[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setYears(String years) {
        this.years = years;
    }

    public ArrayList<ERSubject> getSubjects() {
        return subjects;
    }

    public void setSubjects(ArrayList<ERSubject> subjects) {
        this.subjects = subjects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ERYear)) return false;
        ERYear erYear = (ERYear) o;
        return Objects.equals(group, erYear.group) &&
                Objects.equals(years, erYear.years) &&
                Objects.equals(subjects, erYear.subjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, years, subjects);
    }

    @Override
    public String toString() {
        return "ERYear{" +
                "group='" + group + '\'' +
                ", years='" + years + '\'' +
                ", subjects=" + subjects +
                '}';
    }
}
