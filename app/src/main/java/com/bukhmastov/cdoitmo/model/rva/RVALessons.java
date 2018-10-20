package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLesson;
import com.bukhmastov.cdoitmo.model.schedule.lessons.SLessons;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;

import java.util.Objects;

public class RVALessons extends JsonEntity {

    @JsonProperty("lessons")
    private SLessons lessons;

    @JsonProperty("lesson")
    private SLesson lesson;

    @JsonProperty("weekday")
    private int weekday;

    @JsonProperty("teacher")
    private STeacher teacher;

    public RVALessons() {
        super();
    }

    public RVALessons(SLessons lessons) {
        super();
        this.lessons = lessons;
    }

    public RVALessons(SLesson lesson, int weekday) {
        super();
        this.lesson = lesson;
        this.weekday = weekday;
    }

    public RVALessons(STeacher teacher) {
        super();
        this.teacher = teacher;
    }

    public SLessons getLessons() {
        return lessons;
    }

    public void setLessons(SLessons lessons) {
        this.lessons = lessons;
    }

    public SLesson getLesson() {
        return lesson;
    }

    public int getWeekday() {
        return weekday;
    }

    public void setWeekday(int weekday) {
        this.weekday = weekday;
    }

    public void setLesson(SLesson lesson) {
        this.lesson = lesson;
    }

    public STeacher getTeacher() {
        return teacher;
    }

    public void setTeacher(STeacher teacher) {
        this.teacher = teacher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVALessons)) return false;
        RVALessons that = (RVALessons) o;
        return weekday == that.weekday &&
                Objects.equals(lessons, that.lessons) &&
                Objects.equals(lesson, that.lesson) &&
                Objects.equals(teacher, that.teacher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessons, lesson, weekday, teacher);
    }

    @Override
    public String toString() {
        return "RVALessons{" +
                "lessons=" + lessons +
                ", lesson=" + lesson +
                ", weekday=" + weekday +
                ", teacher=" + teacher +
                '}';
    }
}
