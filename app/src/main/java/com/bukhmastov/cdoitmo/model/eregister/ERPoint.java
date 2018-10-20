package com.bukhmastov.cdoitmo.model.eregister;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;

import java.util.Objects;

public class ERPoint extends JsonEntity {

    @JsonProperty("variable")
    private String name;

    @JsonProperty("max")
    private String max;

    @JsonProperty("limit")
    private String limit;

    @JsonProperty("value")
    private String value;

    public ERPoint() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return NumberUtils.toDouble(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Double getLimit() {
        return NumberUtils.toDouble(limit);
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public Double getMax() {
        return NumberUtils.toDouble(max);
    }

    public void setMax(String max) {
        this.max = max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ERPoint)) return false;
        ERPoint erPoint = (ERPoint) o;
        return Objects.equals(name, erPoint.name) &&
                Objects.equals(max, erPoint.max) &&
                Objects.equals(limit, erPoint.limit) &&
                Objects.equals(value, erPoint.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, max, limit, value);
    }

    @Override
    public String toString() {
        return "ERPoint{" +
                "name='" + name + '\'' +
                ", max='" + max + '\'' +
                ", limit='" + limit + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
