package com.bukhmastov.cdoitmo.model.schedule.attestations;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SAttestation extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("week")
    private String week;

    public SAttestation() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SAttestation)) return false;
        SAttestation that = (SAttestation) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(week, that.week);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, week);
    }

    @Override
    public String toString() {
        return "SAttestation{" +
                "name='" + name + '\'' +
                ", week='" + week + '\'' +
                '}';
    }
}
