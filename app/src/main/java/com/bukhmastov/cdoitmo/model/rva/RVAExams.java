package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.schedule.exams.SExams;
import com.bukhmastov.cdoitmo.model.schedule.exams.SSubject;
import com.bukhmastov.cdoitmo.model.schedule.teachers.STeacher;

import java.util.Objects;

public class RVAExams extends JsonEntity {

    @JsonProperty("exams")
    private SExams exams;

    @JsonProperty("subject")
    private SSubject subject;

    @JsonProperty("teacher")
    private STeacher teacher;

    public RVAExams() {
        super();
    }

    public RVAExams(SExams exams) {
        super();
        this.exams = exams;
    }

    public RVAExams(SSubject subject) {
        super();
        this.subject = subject;
    }

    public RVAExams(STeacher teacher) {
        super();
        this.teacher = teacher;
    }

    public SExams getExams() {
        return exams;
    }

    public void setExams(SExams exams) {
        this.exams = exams;
    }

    public SSubject getSubject() {
        return subject;
    }

    public void setSubject(SSubject subject) {
        this.subject = subject;
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
        if (!(o instanceof RVAExams)) return false;
        RVAExams rvaExams = (RVAExams) o;
        return Objects.equals(exams, rvaExams.exams) &&
                Objects.equals(subject, rvaExams.subject) &&
                Objects.equals(teacher, rvaExams.teacher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exams, subject, teacher);
    }

    @Override
    public String toString() {
        return "RVAExams{" +
                "exams=" + exams +
                ", subject=" + subject +
                ", teacher=" + teacher +
                '}';
    }
}
