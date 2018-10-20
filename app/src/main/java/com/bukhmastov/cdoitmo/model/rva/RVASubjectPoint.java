package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVASubjectPoint extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("mark")
    private String mark;

    public RVASubjectPoint() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVASubjectPoint)) return false;
        RVASubjectPoint that = (RVASubjectPoint) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(desc, that.desc) &&
                Objects.equals(mark, that.mark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, desc, mark);
    }

    @Override
    public String toString() {
        return "RVASubjectPoint{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", desc='" + desc + '\'' +
                ", mark='" + mark + '\'' +
                '}';
    }
}
