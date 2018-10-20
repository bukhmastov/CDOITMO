package com.bukhmastov.cdoitmo.model.rating.pickerall;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RFaculty extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("depId")
    private String depId;

    public RFaculty() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepId() {
        return depId;
    }

    public void setDepId(String depId) {
        this.depId = depId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RFaculty)) return false;
        RFaculty faculty = (RFaculty) o;
        return Objects.equals(name, faculty.name) &&
                Objects.equals(depId, faculty.depId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, depId);
    }

    @Override
    public String toString() {
        return "RFaculty{" +
                "name='" + name + '\'' +
                ", depId='" + depId + '\'' +
                '}';
    }
}
