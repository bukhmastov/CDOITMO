package com.bukhmastov.cdoitmo.model.schedule.lessons;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class SDay extends JsonEntity {

    /**
     * 0 - Понедельник
     * 7 - Воскресенье
     */
    @JsonProperty("weekday")
    private int weekday;

    @JsonProperty("title")
    private String title;

    @JsonProperty("type")
    private String type;

    @JsonProperty("lessons")
    private ArrayList<SLesson> lessons;

    public SDay() {
        super();
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<SLesson> getLessons() {
        return lessons;
    }

    public void setLessons(ArrayList<SLesson> lessons) {
        this.lessons = lessons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SDay)) return false;
        SDay sDay = (SDay) o;
        return weekday == sDay.weekday &&
                Objects.equals(title, sDay.title) &&
                Objects.equals(type, sDay.type) &&
                Objects.equals(lessons, sDay.lessons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, title, type, lessons);
    }

    @Override
    public String toString() {
        return "SDay{" +
                "weekday=" + weekday +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", lessons=" + lessons +
                '}';
    }
}
