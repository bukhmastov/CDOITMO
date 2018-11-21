package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUGroup extends JsonEntity {

    @JsonProperty("group_name")
    private String group;

    @JsonProperty("study_schedule")
    private ArrayList<ISUSchedule> schedule;

    @JsonProperty("exams_schedule")
    private ArrayList<ISUExam> exams;

    public ISUGroup() {
        super();
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ArrayList<ISUSchedule> getSchedule() {
        return schedule;
    }

    public void setSchedule(ArrayList<ISUSchedule> schedule) {
        this.schedule = schedule;
    }

    public ArrayList<ISUExam> getExams() {
        return exams;
    }

    public void setExams(ArrayList<ISUExam> exams) {
        this.exams = exams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUGroup)) return false;
        ISUGroup isuGroup = (ISUGroup) o;
        return Objects.equals(group, isuGroup.group) &&
                Objects.equals(schedule, isuGroup.schedule) &&
                Objects.equals(exams, isuGroup.exams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, schedule, exams);
    }

    @Override
    public String toString() {
        return "ISUGroup{" +
                "group='" + group + '\'' +
                ", schedule=" + schedule +
                ", exams=" + exams +
                '}';
    }
}
