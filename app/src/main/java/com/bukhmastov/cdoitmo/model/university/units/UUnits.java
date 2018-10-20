package com.bukhmastov.cdoitmo.model.university.units;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UUnits extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("unit")
    private UUnit unit;

    @JsonProperty("divisions")
    private ArrayList<UDivision> divisions;

    public UUnits() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public UUnit getUnit() {
        return unit;
    }

    public void setUnit(UUnit unit) {
        this.unit = unit;
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
        if (!(o instanceof UUnits)) return false;
        UUnits uUnits = (UUnits) o;
        return timestamp == uUnits.timestamp &&
                Objects.equals(unit, uUnits.unit) &&
                Objects.equals(divisions, uUnits.divisions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, unit, divisions);
    }

    @Override
    public String toString() {
        return "UUnits{" +
                "timestamp=" + timestamp +
                ", unit=" + unit +
                ", divisions=" + divisions +
                '}';
    }
}
