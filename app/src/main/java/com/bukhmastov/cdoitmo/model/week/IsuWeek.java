package com.bukhmastov.cdoitmo.model.week;

import com.bukhmastov.cdoitmo.model.JsonEntity;
import com.bukhmastov.cdoitmo.model.JsonProperty;

import java.util.Objects;

public class IsuWeek extends JsonEntity {

    /**
     * Number of the week
     */
    @JsonProperty("number")
    private Integer week;

    /**
     * Parity of the week: ["even", "odd"]
     */
    @JsonProperty("parity")
    private String parity;

    public IsuWeek() {
        super();
    }

    public Integer getWeek() {
        return week;
    }

    public String getParity() {
        return parity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IsuWeek)) return false;
        IsuWeek isuWeek = (IsuWeek) o;
        return week == isuWeek.week &&
                Objects.equals(parity, isuWeek.parity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(week, parity);
    }

    @Override
    public String toString() {
        return "IsuWeek{" +
                "week=" + week +
                ", parity='" + parity + '\'' +
                '}';
    }
}
