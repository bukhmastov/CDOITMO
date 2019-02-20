package com.bukhmastov.cdoitmo.model.user.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class IsuUserDataGroup extends JsonEntity {

    @JsonProperty("number")
    private String group;

    @JsonProperty("course")
    private Integer course;

    public String getGroup() {
        return group;
    }

    public Integer getCourse() {
        return course;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IsuUserDataGroup)) return false;
        IsuUserDataGroup that = (IsuUserDataGroup) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(course, that.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, course);
    }

    @Override
    public String toString() {
        return "IsuUserDataGroup{" +
                "group='" + group + '\'' +
                ", course=" + course +
                '}';
    }
}
