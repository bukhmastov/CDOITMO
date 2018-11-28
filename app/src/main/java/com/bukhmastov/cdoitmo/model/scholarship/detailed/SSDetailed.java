package com.bukhmastov.cdoitmo.model.scholarship.detailed;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SSDetailed extends JsonEntity {

    @JsonProperty("contribution")
    private String contribution;

    @JsonProperty("paidTo")
    private String paidTo;

    @JsonProperty("monthOfPayment")
    private String monthOfPayment;

    @JsonProperty("value")
    private String value;

    @JsonProperty("start")
    private String start;

    @JsonProperty("end")
    private String end;

    public SSDetailed() {
        super();
    }

    public String getContribution() {
        return contribution;
    }

    public void setContribution(String contribution) {
        this.contribution = contribution;
    }

    public String getPaidTo() {
        return paidTo;
    }

    public void setPaidTo(String paidTo) {
        this.paidTo = paidTo;
    }

    public String getMonthOfPayment() {
        return monthOfPayment;
    }

    public void setMonthOfPayment(String monthOfPayment) {
        this.monthOfPayment = monthOfPayment;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSDetailed)) return false;
        SSDetailed that = (SSDetailed) o;
        return Objects.equals(contribution, that.contribution) &&
                Objects.equals(paidTo, that.paidTo) &&
                Objects.equals(monthOfPayment, that.monthOfPayment) &&
                Objects.equals(value, that.value) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contribution, paidTo, monthOfPayment, value, start, end);
    }

    @Override
    public String toString() {
        return "SSDetailed{" +
                "contribution='" + contribution + '\'' +
                ", paidTo='" + paidTo + '\'' +
                ", monthOfPayment='" + monthOfPayment + '\'' +
                ", value='" + value + '\'' +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                '}';
    }
}
