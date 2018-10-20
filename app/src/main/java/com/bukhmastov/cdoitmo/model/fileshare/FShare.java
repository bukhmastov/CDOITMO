package com.bukhmastov.cdoitmo.model.fileshare;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class FShare extends JsonEntity {

    @JsonProperty("type")
    private String type;

    @JsonProperty("version")
    private int version;

    public FShare() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FShare)) return false;
        FShare fShare = (FShare) o;
        return Objects.equals(type, fShare.type) &&
                Objects.equals(version, fShare.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version);
    }

    @Override
    public String toString() {
        return "FShare{" +
                "type='" + type + '\'' +
                ", version=" + version +
                '}';
    }
}
