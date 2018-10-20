package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVADualValue extends JsonEntity {

    @JsonProperty("first")
    private String first;

    @JsonProperty("second")
    private String second;

    public RVADualValue() {
        super();
    }

    public RVADualValue(String first, String second) {
        super();
        setFirst(first);
        setSecond(second);
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVADualValue)) return false;
        RVADualValue that = (RVADualValue) o;
        return Objects.equals(first, that.first) &&
                Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "RVADualValue{" +
                "first='" + first + '\'' +
                ", second='" + second + '\'' +
                '}';
    }
}
