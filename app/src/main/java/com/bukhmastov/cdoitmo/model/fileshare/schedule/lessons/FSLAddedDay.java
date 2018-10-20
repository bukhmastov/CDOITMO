package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;

import java.util.Objects;

public class FSLAddedDay extends JsonEntity {

    @JsonProperty("day")
    private int day;

    @JsonProperty("lesson")
    private SLesson lesson;

    public FSLAddedDay() {
        super();
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public SLesson getLesson() {
        return lesson;
    }

    public void setLesson(SLesson lesson) {
        this.lesson = lesson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FSLAddedDay)) return false;
        FSLAddedDay fslAdded = (FSLAddedDay) o;
        return day == fslAdded.day &&
                Objects.equals(lesson, fslAdded.lesson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, lesson);
    }

    @Override
    public String toString() {
        return "FSLAdded{" +
                "day=" + day +
                ", lesson=" + lesson +
                '}';
    }
}
