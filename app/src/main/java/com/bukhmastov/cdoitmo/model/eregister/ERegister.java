package com.bukhmastov.cdoitmo.model.eregister;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.ArrayList;
import java.util.Objects;

public class ERegister extends JsonEntity {

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("years")
    private ArrayList<ERYear> years;

    public ERegister() {
        super();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ArrayList<ERYear> getYears() {
        return years;
    }

    public void setYears(ArrayList<ERYear> years) {
        this.years = years;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ERegister)) return false;
        ERegister eRegister = (ERegister) o;
        return timestamp == eRegister.timestamp && Objects.equals(years, eRegister.years);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, years);
    }

    @Override
    public String toString() {
        return "ERegister{" +
                "timestamp=" + timestamp +
                ", years=" + years +
                '}';
    }
}
