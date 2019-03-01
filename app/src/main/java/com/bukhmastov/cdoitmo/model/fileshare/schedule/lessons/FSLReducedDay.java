package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FSLReducedDay extends JsonEntity {

    @JsonProperty("day")
    private Integer weekday;

    @JsonProperty("custom")
    private String customDay;

    @JsonProperty("lesson")
    private FSLReduced lesson;

    public FSLReducedDay() {
        super();
    }

    public Integer getDay() {
        return weekday;
    }

    public void setDay(Integer day) {
        this.weekday = day;
    }

    public String getCustomDay() {
        return customDay;
    }

    public void setCustomDay(String customDay) {
        this.customDay = customDay;
    }

    public FSLReduced getLesson() {
        return lesson;
    }

    public void setLesson(FSLReduced lesson) {
        this.lesson = lesson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSLReducedDay)) return false;
        FSLReducedDay that = (FSLReducedDay) o;
        return Objects.equals(weekday, that.weekday) &&
                Objects.equals(customDay, that.customDay) &&
                Objects.equals(lesson, that.lesson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, lesson);
    }

    @Override
    public String toString() {
        return "FSLReducedDay{" +
                "weekday=" + weekday +
                ", customDay='" + customDay + '\'' +
                ", lesson=" + lesson +
                '}';
    }
}
