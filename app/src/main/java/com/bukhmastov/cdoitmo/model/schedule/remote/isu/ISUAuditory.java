package com.bukhmastov.cdoitmo.model.schedule.remote.isu;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class ISUAuditory extends JsonEntity {

    /**
     * Type for exam auditory
     * [exam, advice]
     */
    @JsonProperty("type")
    private String type;

    @JsonProperty("auditory_name")
    private String auditoryName;

    @JsonProperty("auditory_address")
    private String auditoryAddress;

    public ISUAuditory() {
        super();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuditoryName() {
        return auditoryName;
    }

    public void setAuditoryName(String auditoryName) {
        this.auditoryName = auditoryName;
    }

    public String getAuditoryAddress() {
        return auditoryAddress;
    }

    public void setAuditoryAddress(String auditoryAddress) {
        this.auditoryAddress = auditoryAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ISUAuditory)) return false;
        ISUAuditory that = (ISUAuditory) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(auditoryName, that.auditoryName) &&
                Objects.equals(auditoryAddress, that.auditoryAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, auditoryName, auditoryAddress);
    }

    @Override
    public String toString() {
        return "ISUAuditory{" +
                "type='" + type + '\'' +
                ", auditoryName='" + auditoryName + '\'' +
                ", auditoryAddress='" + auditoryAddress + '\'' +
                '}';
    }
}
