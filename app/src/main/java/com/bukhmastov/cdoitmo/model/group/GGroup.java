package com.bukhmastov.cdoitmo.model.group;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class GGroup extends JsonEntity {

    @JsonProperty("number")
    private String group;

    @JsonProperty("persons")
    private ArrayList<GPerson> persons;

    public GGroup() {
        super();
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ArrayList<GPerson> getPersons() {
        return persons;
    }

    public void setPersons(ArrayList<GPerson> persons) {
        this.persons = persons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GGroup)) return false;
        GGroup gGroup = (GGroup) o;
        return Objects.equals(group, gGroup.group) &&
                Objects.equals(persons, gGroup.persons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, persons);
    }

    @Override
    public String toString() {
        return "GGroup{" +
                "group='" + group + '\'' +
                ", persons=" + persons +
                '}';
    }
}
