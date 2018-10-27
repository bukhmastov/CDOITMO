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
    private int weekday;

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
    public RVALessons(SLesson lesson, int weekday) {
        super();
        this.lesson = lesson;
        this.weekday = weekday;
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

    public int getWeekday() {
        return weekday;
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
        return weekday == that.weekday &&
                parity == that.parity &&
                Objects.equals(lesson, that.lesson) &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(days, that.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lesson, weekday, teacher, parity, days);
    }

    @Override
    public String toString() {
        return "RVALessons{" +
                "lesson=" + lesson +
                ", weekday=" + weekday +
                ", teacher=" + teacher +
                ", parity=" + parity +
                ", days=" + days +
                '}';
    }
}
