package com.bukhmastov.cdoitmo.model.rva;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;
import com.bukhmastov.cdoitmo.model.eregister.ERSubject;

import java.util.Objects;

public class RVASubject extends JsonEntity {

    @JsonProperty("name")
    private String name;

    @JsonProperty("name")
    private String about;

    @JsonProperty("value")
    private String value;

    @JsonProperty("data")
    private ERSubject subject;

    public RVASubject() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ERSubject getSubject() {
        return subject;
    }

    public void setSubject(ERSubject subject) {
        this.subject = subject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RVASubject)) return false;
        RVASubject that = (RVASubject) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(about, that.about) &&
                Objects.equals(value, that.value) &&
                Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, about, value, subject);
    }

    @Override
    public String toString() {
        return "RVASubject{" +
                "name='" + name + '\'' +
                ", about='" + about + '\'' +
                ", value='" + value + '\'' +
                ", subject=" + subject +
                '}';
    }
}
