package com.bukhmastov.cdoitmo.model.protocol.hash;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class PHash extends JsonEntity {

    @JsonProperty("value")
    private double value;

    @JsonProperty("delta")
    private double delta;

    public PHash() {
        super();
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PHash)) return false;
        PHash pHash = (PHash) o;
        return value == pHash.value &&
                delta == pHash.delta;
    }

    @Override
    public int hashCode() {

        return Objects.hash(value, delta);
    }

    @Override
    public String toString() {
        return "PHash{" +
                "value=" + value +
                ", delta=" + delta +
                '}';
    }
}
