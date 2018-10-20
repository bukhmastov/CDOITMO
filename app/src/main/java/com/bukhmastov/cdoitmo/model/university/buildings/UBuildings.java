package com.bukhmastov.cdoitmo.model.university.buildings;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class UBuildings extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("type_id")
    private int typeId;

    @JsonProperty("major")
    private int major;

    @JsonProperty("list")
    private ArrayList<UBuilding> buildings;

    public UBuildings() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public ArrayList<UBuilding> getBuildings() {
        return buildings;
    }

    public void setBuildings(ArrayList<UBuilding> buildings) {
        this.buildings = buildings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UBuildings)) return false;
        UBuildings that = (UBuildings) o;
        return timestamp == that.timestamp &&
                typeId == that.typeId &&
                major == that.major &&
                Objects.equals(buildings, that.buildings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, typeId, major, buildings);
    }

    @Override
    public String toString() {
        return "UBuildings{" +
                "timestamp=" + timestamp +
                ", typeId=" + typeId +
                ", major=" + major +
                ", buildings=" + buildings +
                '}';
    }
}
