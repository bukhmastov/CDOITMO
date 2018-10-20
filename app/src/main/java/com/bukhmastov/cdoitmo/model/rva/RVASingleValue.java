package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVASingleValue extends JsonEntity {

    @JsonProperty("value")
    private String value;

    public RVASingleValue() {
        super();
    }

    public RVASingleValue(String value) {
        super();
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVASingleValue)) return false;
        RVASingleValue that = (RVASingleValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "RVASingleValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
