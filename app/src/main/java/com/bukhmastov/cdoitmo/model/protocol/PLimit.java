package com.bukhmastov.cdoitmo.model.protocol;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class PLimit extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("min")
    private String min;

    @JsonProperty("max")
    private String max;

    @JsonProperty("threshold")
    private String threshold;

    public PLimit() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PLimit)) return false;
        PLimit limit = (PLimit) o;
        return Objects.equals(name, limit.name) &&
                Objects.equals(min, limit.min) &&
                Objects.equals(max, limit.max) &&
                Objects.equals(threshold, limit.threshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, min, max, threshold);
    }

    @Override
    public String toString() {
        return "PLimit{" +
                "name='" + name + '\'' +
                ", min='" + min + '\'' +
                ", max='" + max + '\'' +
                ", threshold='" + threshold + '\'' +
                '}';
    }
}
