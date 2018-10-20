package com.bukhmastov.cdoitmo.model.schedule.lessons.reduced;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SDayReduced extends JsonEntity {

    @JsonProperty("weekday")
    private int weekday;

    @JsonProperty("lessons")
    private ArrayList<String> lessons;

    public SDayReduced() {
        super();
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public ArrayList<String> getLessons() {
        return lessons;
    }

    public void setLessons(ArrayList<String> lessons) {
        this.lessons = lessons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDayReduced)) return false;
        SDayReduced that = (SDayReduced) o;
        return weekday == that.weekday &&
                Objects.equals(lessons, that.lessons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, lessons);
    }

    @Override
    public String toString() {
        return "SDayReduced{" +
                "weekday=" + weekday +
                ", lessons=" + lessons +
                '}';
    }
}
