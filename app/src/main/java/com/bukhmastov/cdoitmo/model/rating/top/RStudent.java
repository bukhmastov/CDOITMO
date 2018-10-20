package com.bukhmastov.cdoitmo.model.rating.top;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RStudent extends JsonEntity {

    @JsonProperty("number")
    private int number;

    @JsonProperty("fio")
    private String fio;

    @JsonProperty("group")
    private String group;

    @JsonProperty("department")
    private String department;

    @JsonProperty("is_me")
    private boolean isMe;

    @JsonProperty("change")
    private String change;

    @JsonProperty("delta")
    private String delta;

    public RStudent() {
        super();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RStudent)) return false;
        RStudent student = (RStudent) o;
        return number == student.number &&
                Objects.equals(fio, student.fio) &&
                Objects.equals(group, student.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, fio, group);
    }

    @Override
    public String toString() {
        return "RStudent{" +
                "number=" + number +
                ", fio='" + fio + '\'' +
                ", group='" + group + '\'' +
                ", department='" + department + '\'' +
                ", isMe=" + isMe +
                ", change='" + change + '\'' +
                ", delta='" + delta + '\'' +
                '}';
    }
}
