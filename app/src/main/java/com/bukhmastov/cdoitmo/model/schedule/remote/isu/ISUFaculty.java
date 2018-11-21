package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUFaculty extends JsonEntity {

    @JsonProperty("faculty_name")
    private String facultyName;

    @JsonProperty("faculty_id")
    private int facultyId;

    @JsonProperty("departments")
    private ArrayList<ISUDepartment> departments;

    public ISUFaculty() {
        super();
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public int getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(int facultyId) {
        this.facultyId = facultyId;
    }

    public ArrayList<ISUDepartment> getDepartments() {
        return departments;
    }

    public void setDepartments(ArrayList<ISUDepartment> departments) {
        this.departments = departments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUFaculty)) return false;
        ISUFaculty that = (ISUFaculty) o;
        return facultyId == that.facultyId &&
                Objects.equals(facultyName, that.facultyName) &&
                Objects.equals(departments, that.departments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facultyName, facultyId, departments);
    }

    @Override
    public String toString() {
        return "ISUFaculty{" +
                "facultyName='" + facultyName + '\'' +
                ", facultyId=" + facultyId +
                ", departments=" + departments +
                '}';
    }
}
