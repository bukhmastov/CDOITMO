package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class RVAProtocolChange extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("meta")
    private String meta;

    @JsonProperty("value")
    private String value;

    @JsonProperty("delta")
    private String delta;

    @JsonProperty("delta_exists")
    private boolean isDeltaExists;

    @JsonProperty("delta_negative")
    private boolean IsDeltaNegative;

    public RVAProtocolChange() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public boolean isDeltaExists() {
        return isDeltaExists;
    }

    public void setDeltaExists(boolean deltaExists) {
        isDeltaExists = deltaExists;
    }

    public boolean isDeltaNegative() {
        return IsDeltaNegative;
    }

    public void setDeltaNegative(boolean deltaNegative) {
        IsDeltaNegative = deltaNegative;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVAProtocolChange)) return false;
        RVAProtocolChange that = (RVAProtocolChange) o;
        return isDeltaExists == that.isDeltaExists &&
                IsDeltaNegative == that.IsDeltaNegative &&
                Objects.equals(name, that.name) &&
                Objects.equals(desc, that.desc) &&
                Objects.equals(meta, that.meta) &&
                Objects.equals(value, that.value) &&
                Objects.equals(delta, that.delta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, desc, meta, value, delta, isDeltaExists, IsDeltaNegative);
    }

    @Override
    public String toString() {
        return "RVAProtocolChange{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", meta='" + meta + '\'' +
                ", value='" + value + '\'' +
                ", delta='" + delta + '\'' +
                ", isDeltaExists=" + isDeltaExists +
                ", IsDeltaNegative=" + IsDeltaNegative +
                '}';
    }
}
