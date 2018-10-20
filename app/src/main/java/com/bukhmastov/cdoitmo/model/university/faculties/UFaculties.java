package com.bukhmastov.cdoitmo.model.university.faculties;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UFaculties extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("structure")
    private UStructure structure;

    @JsonProperty("divisions")
    private ArrayList<UDivision> divisions;

    public UFaculties() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public UStructure getStructure() {
        return structure;
    }

    public void setStructure(UStructure structure) {
        this.structure = structure;
    }

    public ArrayList<UDivision> getDivisions() {
        return divisions;
    }

    public void setDivisions(ArrayList<UDivision> divisions) {
        this.divisions = divisions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UFaculties)) return false;
        UFaculties that = (UFaculties) o;
        return timestamp == that.timestamp &&
                Objects.equals(structure, that.structure) &&
                Objects.equals(divisions, that.divisions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, structure, divisions);
    }

    @Override
    public String toString() {
        return "UFaculties{" +
                "timestamp=" + timestamp +
                ", structure=" + structure +
                ", divisions=" + divisions +
                '}';
    }
}
