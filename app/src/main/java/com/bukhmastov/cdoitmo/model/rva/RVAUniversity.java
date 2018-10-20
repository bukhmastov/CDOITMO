package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVAUniversity extends JsonEntity {

    private JsonEntity entity;
    private Object value;

    public RVAUniversity(JsonEntity entity) {
        super();
        this.entity = entity;
    }

    public RVAUniversity(Object value) {
        super();
        this.value = value;
    }

    public JsonEntity getEntity() {
        return entity;
    }

    public String getValueString() {
        return value instanceof String ? (String) value : null;
    }

    public Integer getValueInteger() {
        return value instanceof Integer ? (Integer) value : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVAUniversity)) return false;
        RVAUniversity that = (RVAUniversity) o;
        return Objects.equals(entity, that.entity) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, value);
    }

    @Override
    public String toString() {
        return "RVAUniversity{" +
                "entity=" + entity +
                ", value='" + value + '\'' +
                '}';
    }
}
