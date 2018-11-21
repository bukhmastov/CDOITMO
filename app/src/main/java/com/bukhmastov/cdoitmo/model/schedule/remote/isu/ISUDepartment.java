package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ISUDepartment extends JsonEntity {

    @JsonProperty("department_name")
    private String departmentName;

    @JsonProperty("department_id")
    private int departmentId;

    @JsonProperty("groups")
    private ArrayList<ISUGroup> groups;

    public ISUDepartment() {
        super();
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public ArrayList<ISUGroup> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<ISUGroup> groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUDepartment)) return false;
        ISUDepartment that = (ISUDepartment) o;
        return departmentId == that.departmentId &&
                Objects.equals(departmentName, that.departmentName) &&
                Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentName, departmentId, groups);
    }

    @Override
    public String toString() {
        return "ISUDepartment{" +
                "departmentName='" + departmentName + '\'' +
                ", departmentId=" + departmentId +
                ", groups=" + groups +
                '}';
    }
}
