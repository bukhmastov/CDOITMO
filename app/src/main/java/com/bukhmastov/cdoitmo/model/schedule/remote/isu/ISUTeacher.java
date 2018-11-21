package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ISUTeacher extends JsonEntity {

    @JsonProperty("teacher_name")
    private String teacherName;

    @JsonProperty("teacher_id")
    private int teacherId;

    public ISUTeacher() {
        super();
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUTeacher)) return false;
        ISUTeacher that = (ISUTeacher) o;
        return teacherId == that.teacherId &&
                Objects.equals(teacherName, that.teacherName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherName, teacherId);
    }

    @Override
    public String toString() {
        return "ISUTeacher{" +
                "teacherName='" + teacherName + '\'' +
                ", teacherId=" + teacherId +
                '}';
    }
}
