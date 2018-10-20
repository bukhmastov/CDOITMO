package com.bukhmastov.cdoitmo.model.protocol;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class PChange extends JsonEntity {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("sign")
    private String teacher;

    @JsonProperty("date")
    private String date;

    @JsonProperty("value")
    private String value;

    @JsonProperty("var")
    private PLimit var;

    @JsonProperty("cdoitmo_hash")
    private String cdoitmoHash;

    @JsonProperty("cdoitmo_delta")
    private String cdoitmoDelta;

    @JsonProperty("cdoitmo_delta_double")
    private Double cdoitmoDeltaDouble;

    public PChange() {
        super();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PLimit getVar() {
        return var;
    }

    public PLimit getVarSafely() {
        if (var == null) {
            var = new PLimit();
        }
        return var;
    }

    public void setVar(PLimit var) {
        this.var = var;
    }

    public String getName() {
        return getVarSafely().getName();
    }

    public void setName(String name) {
        getVarSafely().setName(name);
    }

    public String getMin() {
        return getVarSafely().getMin();
    }

    public void setMin(String min) {
        getVarSafely().setMin(min);
    }

    public String getMax() {
        return getVarSafely().getMax();
    }

    public void setMax(String max) {
        getVarSafely().setMax(max);
    }

    public String getThreshold() {
        return getVarSafely().getThreshold();
    }

    public void setThreshold(String threshold) {
        getVarSafely().setThreshold(threshold);
    }

    public String getCdoitmoHash() {
        return cdoitmoHash;
    }

    public void setCdoitmoHash(String cdoitmoHash) {
        this.cdoitmoHash = cdoitmoHash;
    }

    public String getCdoitmoDelta() {
        return cdoitmoDelta;
    }

    public void setCdoitmoDelta(String cdoitmoDelta) {
        this.cdoitmoDelta = cdoitmoDelta;
    }

    public Double getCdoitmoDeltaDouble() {
        return cdoitmoDeltaDouble;
    }

    public void setCdoitmoDeltaDouble(Double cdoitmoDeltaDouble) {
        this.cdoitmoDeltaDouble = cdoitmoDeltaDouble;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PChange)) return false;
        PChange change = (PChange) o;
        return Objects.equals(subject, change.subject) &&
                Objects.equals(teacher, change.teacher) &&
                Objects.equals(date, change.date) &&
                Objects.equals(value, change.value) &&
                Objects.equals(var, change.var);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, teacher, date, value, var);
    }

    @Override
    public String toString() {
        return "PChange{" +
                "subject='" + subject + '\'' +
                ", teacher='" + teacher + '\'' +
                ", date='" + date + '\'' +
                ", value='" + value + '\'' +
                ", var=" + var +
                ", cdoitmoHash='" + cdoitmoHash + '\'' +
                ", cdoitmoDelta='" + cdoitmoDelta + '\'' +
                ", cdoitmoDeltaDouble=" + cdoitmoDeltaDouble +
                '}';
    }
}
