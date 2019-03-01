package com.bukhmastov.cdoitmo.model.fileshare.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;

import java.util.Objects;

public class FSLAddedDay extends JsonEntity {

    @JsonProperty("day")
    private Integer weekday;

    @JsonProperty("custom")
    private String customDay;

    @JsonProperty("lesson")
    private SLesson lesson;

    public FSLAddedDay() {
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
        return Objects.equals(weekday, fslAdded.weekday) &&
                Objects.equals(customDay, fslAdded.customDay) &&
                Objects.equals(lesson, fslAdded.lesson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, lesson);
    }

    @Override
    public String toString() {
        return "FSLAdded{" +
                "weekday=" + weekday +
                ", customDay='" + customDay + '\'' +
                ", lesson=" + lesson +
                '}';
    }
}
