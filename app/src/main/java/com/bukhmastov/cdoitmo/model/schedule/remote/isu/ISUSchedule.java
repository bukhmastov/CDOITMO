package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUSchedule extends JsonEntity {

    /**
     * 0 - Расписание на определенный день
     * 1 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("weekday")
    private Integer weekday;

    @JsonProperty("lessons")
    private ArrayList<ISULesson> lessons;

    public ISUSchedule() {
        super();
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
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
        return Objects.equals(weekday, that.weekday) &&
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
