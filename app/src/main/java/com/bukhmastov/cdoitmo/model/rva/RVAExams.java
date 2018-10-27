package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;

import java.util.ArrayList;
import java.util.Objects;

public class RVAExams extends JsonEntity {

    @JsonProperty("subject")
    private SSubject subject;

    @JsonProperty("teacher")
    private STeacher teacher;

    @JsonProperty("events")
    private ArrayList<SSubject> events;

    public RVAExams() {
        super();
    }

    // exam click
    public RVAExams(SSubject subject) {
        super();
        this.subject = subject;
    }

    // teacher click
    public RVAExams(STeacher teacher) {
        super();
        this.teacher = teacher;
    }

    // menu share click
    public RVAExams(ArrayList<SSubject> events) {
        super();
        this.events = events;
    }

    public SSubject getSubject() {
        return subject;
    }

    public STeacher getTeacher() {
        return teacher;
    }

    public ArrayList<SSubject> getEvents() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVAExams)) return false;
        RVAExams rvaExams = (RVAExams) o;
        return Objects.equals(subject, rvaExams.subject) &&
                Objects.equals(teacher, rvaExams.teacher) &&
                Objects.equals(events, rvaExams.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, teacher, events);
    }

    @Override
    public String toString() {
        return "RVAExams{" +
                "subject=" + subject +
                ", teacher=" + teacher +
                ", events=" + events +
                '}';
    }
}
