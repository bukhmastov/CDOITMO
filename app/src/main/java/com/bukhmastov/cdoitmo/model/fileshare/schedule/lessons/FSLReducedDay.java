package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FSLReducedDay extends JsonEntity {

    @JsonProperty("day")
    private int day;

    @JsonProperty("lesson")
    private FSLReduced lesson;

    public FSLReducedDay() {
        super();
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
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
        return day == that.day &&
                Objects.equals(lesson, that.lesson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, lesson);
    }

    @Override
    public String toString() {
        return "FSLReducedDay{" +
                "day=" + day +
                ", lesson=" + lesson +
                '}';
    }
}
