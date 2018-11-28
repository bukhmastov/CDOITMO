package com.bukhmastov.cdoitmo.model.scholarship.paid;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class SSPaid extends JsonEntity {

    @JsonProperty("rn")
    private int number;

    @JsonProperty("value")
    private String value;

    @JsonProperty("month")
    private int month;

    @JsonProperty("year")
    private int year;

    public SSPaid() {
        super();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SSPaid)) return false;
        SSPaid ssPaid = (SSPaid) o;
        return number == ssPaid.number &&
                month == ssPaid.month &&
                year == ssPaid.year &&
                Objects.equals(value, ssPaid.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, value, month, year);
    }

    @Override
    public String toString() {
        return "SSPaid{" +
                "number=" + number +
                ", value='" + value + '\'' +
                ", month=" + month +
                ", year=" + year +
                '}';
    }
}
