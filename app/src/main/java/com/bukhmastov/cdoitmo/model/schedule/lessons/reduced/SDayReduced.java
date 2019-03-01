package com.bukhmastov.cdoitmo.model.schedule.lessons.reduced;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SDayReduced extends JsonEntity {

    @JsonProperty("weekday")
    private Integer weekday;

    @JsonProperty("custom")
    private String customDay;

    @JsonProperty("lessons")
    private ArrayList<String> lessons;

    public SDayReduced() {
        super();
    }

    public SDayReduced(int weekday, String lesson) {
        super();
        this.weekday = weekday;
        this.customDay = "";
        this.lessons = new ArrayList<>();
        this.lessons.add(lesson);
    }

    public SDayReduced(String customDay, String lesson) {
        super();
        this.weekday = null;
        this.customDay = customDay;
        this.lessons = new ArrayList<>();
        this.lessons.add(lesson);
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
        this.weekday = weekday;
    }

    public String getCustomDay() {
        return customDay;
    }

    public void setCustomDay(String customDay) {
        this.customDay = customDay;
    }

    public ArrayList<String> getLessons() {
        return lessons;
    }

    public void setLessons(ArrayList<String> lessons) {
        this.lessons = lessons;
    }

    public boolean isMatched(Integer weekday, String customDay) {
        if (weekday != null && this.weekday != null) {
            return Objects.equals(this.weekday, weekday);
        } else {
            if (customDay == null) {
                return false;
            }
            return Objects.equals(this.customDay, customDay);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDayReduced)) return false;
        SDayReduced that = (SDayReduced) o;
        return Objects.equals(weekday, that.weekday) &&
                Objects.equals(customDay, that.customDay) &&
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
                ", customDay='" + customDay + '\'' +
                ", lessons=" + lessons +
                '}';
    }
}
