package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUSchedule extends JsonEntity {

    /**
     * 0 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("weekday")
    private int weekday;

    @JsonProperty("lessons")
    private ArrayList<ISULesson> lessons;

    public ISUSchedule() {
        super();
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public ArrayList<ISULesson> getLessons() {
        return lessons;
    }

    public void setLessons(ArrayList<ISULesson> lessons) {
        this.lessons = lessons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUSchedule)) return false;
        ISUSchedule that = (ISUSchedule) o;
        return weekday == that.weekday &&
                Objects.equals(lessons, that.lessons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, lessons);
    }

    @Override
    public String toString() {
        return "ISUSchedule{" +
                "weekday=" + weekday +
                ", lessons=" + lessons +
                '}';
    }
}
