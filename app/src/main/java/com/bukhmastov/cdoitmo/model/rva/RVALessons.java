package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SDay;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;

import java.util.ArrayList;
import java.util.Objects;

public class RVALessons extends JsonEntity {

    @JsonProperty("lesson")
    private SLesson lesson;

    @JsonProperty("weekday")
    private Integer weekday;

    @JsonProperty("customDay")
    private String customDay;

    @JsonProperty("teacher")
    private STeacher teacher;

    @JsonProperty("parity")
    private int parity;

    @JsonProperty("parity")
    private ArrayList<SDay> days;

    public RVALessons() {
        super();
    }

    // lesson click
    public RVALessons(SLesson lesson, Integer weekday, String customDay) {
        super();
        this.lesson = lesson;
        this.weekday = weekday;
        this.customDay = customDay;
    }

    // teacher click
    public RVALessons(STeacher teacher) {
        super();
        this.teacher = teacher;
    }

    // menu create click
    public RVALessons(int parity) {
        super();
        this.parity = parity;
    }

    // menu share click
    public RVALessons(ArrayList<SDay> days) {
        super();
        this.days = days;
    }

    public SLesson getLesson() {
        return lesson;
    }

    public Integer getWeekday() {
        return weekday;
    }

    public String getCustomDay() {
        return customDay;
    }

    public STeacher getTeacher() {
        return teacher;
    }

    public int getParity() {
        return parity;
    }

    public ArrayList<SDay> getDays() {
        return days;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVALessons)) return false;
        RVALessons that = (RVALessons) o;
        return Objects.equals(weekday, that.weekday) &&
                Objects.equals(customDay, that.customDay) &&
                parity == that.parity &&
                Objects.equals(lesson, that.lesson) &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(days, that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lesson, weekday, customDay, teacher, parity, days);
    }

    @Override
    public String toString() {
        return "RVALessons{" +
                "lesson=" + lesson +
                ", weekday=" + weekday +
                ", customDay='" + customDay + '\'' +
                ", teacher=" + teacher +
                ", parity=" + parity +
                ", days=" + days +
                '}';
    }
}
